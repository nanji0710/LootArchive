#!/usr/bin/env bash
# 本地演练 .github/workflows/build.yml 里的纯 shell 逻辑（不跑 Gradle 构建本身），
# 目的是在推送之前就发现「镜像替换 / 版本号一致性 / Tag 校验 / 签名指纹解析」这些问题。
#
# CI 上用的是 jq 与 GNU sed/grep -P（ubuntu-latest 自带）。本机 Git Bash 通常没有 jq，
# 故这里用 node 解析同一份 JSON，结果一致；grep -P 在 Git Bash 上可用。
#
# 用法：在仓库根执行  bash .github/verify-workflow-logic.sh
# 退出码 0 表示全部通过。
set -uo pipefail

REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO" || { echo "找不到仓库根目录"; exit 1; }
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
FAIL=0

pass() { echo "  PASS  $1"; }
fail() { echo "  FAIL  $1"; FAIL=$((FAIL+1)); }

# node 在 Git Bash 下仍是 Windows 程序，认不了 /d/... 形式的 POSIX 路径，需要转成盘符形式
if command -v cygpath >/dev/null 2>&1; then REPO_NATIVE="$(cygpath -m "$REPO")"; else REPO_NATIVE="$REPO"; fi
jget() { node -e "console.log(require('${REPO_NATIVE}/version.json')['$1'])"; }

echo "仓库根：$REPO"

echo
echo "=== 1. 镜像替换（在临时副本上做，不动仓库文件）==="
cp gradle/wrapper/gradle-wrapper.properties "$WORK/gwp"
cp settings.gradle.kts "$WORK/settings.gradle.kts"
sed -i 's|mirrors\.huaweicloud\.com/gradle/|services.gradle.org/distributions/|' "$WORK/gwp"
sed -i 's|^networkTimeout=.*|networkTimeout=60000|' "$WORK/gwp"
sed -i '/maven\.aliyun\.com/d' "$WORK/settings.gradle.kts"

DIST=$(grep '^distributionUrl' "$WORK/gwp")
TIMEOUT=$(grep '^networkTimeout' "$WORK/gwp")
echo "  $DIST"
echo "  $TIMEOUT"
case "$DIST" in
  *services.gradle.org/distributions/gradle-8.9-bin.zip) pass "distributionUrl 已指向官方地址，版本仍是 8.9" ;;
  *) fail "distributionUrl 替换结果不对：$DIST" ;;
esac
case "$TIMEOUT" in
  networkTimeout=60000) pass "networkTimeout 已放宽到 60s" ;;
  *) fail "networkTimeout 未替换" ;;
esac
if grep -q 'aliyun' "$WORK/settings.gradle.kts"; then
  fail "settings.gradle.kts 里仍残留阿里云镜像"
else
  pass "settings.gradle.kts 里阿里云镜像已移除"
fi
grep -q 'google()' "$WORK/settings.gradle.kts" && grep -q 'mavenCentral()' "$WORK/settings.gradle.kts" \
  && pass "官方仓库 google() / mavenCentral() 仍在" || fail "官方仓库被误删"
# 删行后括号必须仍然配平，否则 CI 上会直接语法错误
OB=$(tr -cd '{' < "$WORK/settings.gradle.kts" | wc -c)
CB=$(tr -cd '}' < "$WORK/settings.gradle.kts" | wc -c)
[ "$OB" = "$CB" ] && pass "删行后花括号配平（$OB 对）" || fail "删行后花括号不配平：{ ×$OB vs } ×$CB"

echo
echo "=== 2. 版本号一致性（version.json vs app/build.gradle.kts）==="
NAME_JSON=$(jget versionName | tr -d '\r')
CODE_JSON=$(jget versionCode | tr -d '\r')
URL_JSON=$(jget apkDownloadUrl | tr -d '\r')
NAME_GRADLE=$(grep -oP 'versionName\s*=\s*"\K[^"]+' app/build.gradle.kts | head -n1)
CODE_GRADLE=$(grep -oP 'versionCode\s*=\s*\K[0-9]+' app/build.gradle.kts | head -n1)
echo "  version.json     -> name=$NAME_JSON code=$CODE_JSON"
echo "  build.gradle.kts -> name=$NAME_GRADLE code=$CODE_GRADLE"
[ "$NAME_JSON" = "$NAME_GRADLE" ] && pass "versionName 一致" || fail "versionName 不一致"
[ "$CODE_JSON" = "$CODE_GRADLE" ] && pass "versionCode 一致" || fail "versionCode 不一致"

echo
echo "=== 3. 产物文件名与下载地址自洽 ==="
EXPECTED_APK="LootArchive-release-v${NAME_GRADLE}.apk"
case "$URL_JSON" in
  *"/$EXPECTED_APK") pass "下载地址文件名匹配：$EXPECTED_APK" ;;
  *) fail "下载地址文件名不匹配，期望以 /$EXPECTED_APK 结尾，实际 $URL_JSON" ;;
esac
# App 内置的输出名规则见 app/build.gradle.kts 的 applicationVariants.all
grep -q 'LootArchive-\${buildType.name}-v\${defaultConfig.versionName}.apk' app/build.gradle.kts \
  && pass "构建产物命名规则与预期一致" || fail "app/build.gradle.kts 的产物命名规则变了，需同步本脚本"

echo
echo "=== 4. Tag 校验逻辑（Tag 名 / 版本号 / 下载地址三者自洽）==="
check_tag() {
  local ref="$1" expect_fail="$2"
  local expected_tag="V${NAME_JSON}"
  local rc=0
  [ "$ref" != "$expected_tag" ] && rc=1
  case "$URL_JSON" in
    *"/$ref/"*) : ;;
    *) rc=1 ;;
  esac
  if [ "$expect_fail" = "yes" ]; then
    [ "$rc" -ne 0 ] && pass "Tag $ref 被正确拒绝（应拒绝）" || fail "Tag $ref 本应被拒绝却放行了"
  else
    [ "$rc" -eq 0 ] && pass "Tag $ref 被正确接受" || fail "Tag $ref 本应通过却被拒"
  fi
}
check_tag "V6.11.0" no
check_tag "v6.11.0" yes   # 小写 v：App 内下载地址用的是大写 V，Tag 必须一致
check_tag "V6.10.1" yes   # 旧版本号：与当前版本号不符
check_tag "V6.12.0" yes   # 版本号超前，但 version.json 的地址里还没这个 Tag

echo
echo "=== 5. 签名指纹解析（拿真实 APK 实测 apksigner 输出格式）==="
APK="app/build/outputs/apk/release/LootArchive-release-v${NAME_GRADLE}.apk"
SIGNER="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}}/build-tools/36.0.0/apksigner"
[ -x "$SIGNER" ] || SIGNER="$SIGNER.bat"
if [ -f "$APK" ] && [ -e "$SIGNER" ]; then
  ACTUAL=$("$SIGNER" verify --print-certs "$APK" 2>/dev/null | grep -m1 -i 'SHA-256 digest' | awk '{print $NF}' | tr -d '\r\n')
  EXPECTED="6803e69fd87adfa9d71b70a8be37429ff3425cb7061dca8c27663728b95bb093"
  echo "  parsed: $ACTUAL"
  [ "$ACTUAL" = "$EXPECTED" ] && pass "指纹解析正确，且与已发布版本同一把签名" || fail "指纹解析结果与期望不符"
else
  echo "  跳过（缺 APK 或 apksigner）。先执行 :app:assembleRelease 并设置 ANDROID_HOME 可启用本项。"
fi

echo
echo "=== 6. gradlew 在 Linux runner 上可直接执行 ==="
head -c 2 gradlew | od -An -tx1 | tr -d ' \n' | grep -q '^23' && pass "gradlew 以 #! 开头（无 BOM）" || fail "gradlew 不是以 #! 开头，可能有 BOM"
if grep -q $'\r' gradlew; then fail "gradlew 含 CR 换行，Linux 会报 bad interpreter"; else pass "gradlew 是 LF 换行"; fi
MODE=$(git ls-files -s gradlew | awk '{print $1}')
[ "$MODE" = "100755" ] && pass "gradlew 在 git 索引里带可执行位（100755）" || fail "gradlew 缺少可执行位（当前 $MODE）"

echo
echo "=== 7. Release 说明来自 version.json 的 updateLog ==="
LINES=$(node -e "console.log(require('${REPO_NATIVE}/version.json').updateLog)" | wc -l)
node -e "console.log(require('${REPO_NATIVE}/version.json').updateLog)" | head -2 | sed 's/^/    /'
[ "$LINES" -ge 3 ] && pass "updateLog 可转为多行 Release 说明（$LINES 行）" || fail "updateLog 行数异常"

echo
echo "=== 8. 工作流引用的 Secret 名字 ==="
for s in LOOTARCHIVE_KEYSTORE_BASE64 LOOTARCHIVE_KEYSTORE_PASSWORD LOOTARCHIVE_KEY_ALIAS LOOTARCHIVE_KEY_PASSWORD; do
  grep -q "$s" .github/workflows/build.yml && pass "引用了 $s" || fail "缺少 $s"
done
# 这 4 个名字必须与 app/build.gradle.kts 里 prop() 读取的名字完全一致，否则签名静默回退到 debug keystore
for s in LOOTARCHIVE_KEYSTORE_PATH LOOTARCHIVE_KEYSTORE_PASSWORD LOOTARCHIVE_KEY_ALIAS LOOTARCHIVE_KEY_PASSWORD; do
  grep -q "\"$s\"" app/build.gradle.kts && pass "app/build.gradle.kts 读取 $s" || fail "app/build.gradle.kts 未读取 $s"
done

echo
echo "=== 9. 工作流本身可被 YAML 解析 ==="
node -e "
const fs=require('fs');const t=fs.readFileSync('.github/workflows/build.yml','utf8');
// 只做基础结构检查：CI 上的 YAML 由 GitHub 解析，这里确保没有明显的制表符缩进
if(/\t/.test(t)){console.error('含制表符缩进');process.exit(1);}
if(!/^on:/m.test(t)){console.error('缺少 on:');process.exit(1);}
if(!/^jobs:/m.test(t)){console.error('缺少 jobs:');process.exit(1);}
console.log('  基本结构 OK，行数='+t.split('\n').length);
" && pass "YAML 基本结构正常（无制表符、含 on: 与 jobs:）" || fail "YAML 基本结构异常"

echo
echo "=== 10. 有/无签名密钥两条路径的产物分支 ==="
# 有密钥 ⇒ 提交 main 也构建并上传 Release APK，不传 Debug；
# 无密钥 ⇒ 退回 Debug APK（只验证能否编译）；
# 打 Tag ⇒ 必须有密钥，否则直接失败，绝不发布错签名的包。
WF=.github/workflows/build.yml
have_release_build=$(grep -c "startsWith(github.ref, 'refs/tags/') || steps.sign.outputs.have == '1'" "$WF")
[ "$have_release_build" -ge 2 ] && pass "Release 构建与指纹校验都由「打 Tag 或已有密钥」触发（$have_release_build 处）" || fail "Release 构建/指纹校验的触发条件不完整"

grep -q "steps.sign.outputs.have == '1'" "$WF" && pass "准备签名文件仅在已配置密钥时执行" || fail "准备签名文件缺少密钥判断"

grep -q "steps.sign.outputs.have != '1'" "$WF" && pass "无密钥时走 Debug 兜底路径" || fail "无密钥兜底路径缺失"

grep -q "构建 Release APK" "$WF" && grep -q "上传 Release APK" "$WF" && pass "提交到 main 也有 Release APK 的上传步骤" || fail "提交到 main 缺少 Release APK 上传步骤"

# 打 Tag 但没密钥：必须有一条硬失败，否则会发布用 debug keystore 签的包
node -e "
const fs=require('fs');const t=fs.readFileSync('.github/workflows/build.yml','utf8');
const m=t.match(/- name: 校验发布用的签名密钥[\s\S]*?exit 1/);
process.exit(m?0:1);
" && pass "打 Tag 但缺密钥会硬失败，不会发布错签名的包" || fail "打 Tag 缺密钥没有硬失败保护"

echo
if [ "$FAIL" -eq 0 ]; then echo "==> 全部通过"; else echo "==> 有 $FAIL 项失败"; fi
exit "$FAIL"
