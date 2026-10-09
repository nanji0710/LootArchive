#!/usr/bin/env bash
# 本地演练 .github/workflows/build.yml 里的纯 shell 逻辑（不跑 Gradle 构建本身），
# 目的是在推送之前就发现「镜像替换 / 版本号一致性 / Tag 决策 / 签名指纹解析」这些问题。
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
WF=.github/workflows/build.yml

pass() { echo "  PASS  $1"; }
fail() { echo "  FAIL  $1"; FAIL=$((FAIL+1)); }

# node 在 Git Bash 下仍是 Windows 程序，认不了 /d/... 形式的 POSIX 路径，需要转成盘符形式
if command -v cygpath >/dev/null 2>&1; then REPO_NATIVE="$(cygpath -m "$REPO")"; else REPO_NATIVE="$REPO"; fi
jget() { node -e "console.log(require('${REPO_NATIVE}/version.json')['$1'])"; }

# 把工作流里某个 step 的 run: 块原样取出来（去掉缩进），用于真跑一遍它的 shell
extract_run() {
  awk -v want="$1" '
    {
      line = $0
      sub(/^[[:space:]]+/, "", line)
    }
    line == "- name: " want { hit = 1; next }
    hit && line ~ /^- name: / { exit }
    hit && line == "run: |" { inrun = 1; next }
    inrun {
      if ($0 ~ /^[[:space:]]*$/) { print ""; next }
      if (substr($0, 1, 10) == "          ") { print substr($0, 11); next }
      exit
    }
  ' "$WF"
}

# 以「宽松解析」把工作流的所有 step 列成 JSON，供后面断言触发条件
steps_json() {
  node -e "
    const fs=require('fs');
    const lines=fs.readFileSync('$WF','utf8').split(/\r?\n/);
    const out=[]; let cur=null;
    for(const l of lines){
      let m;
      if((m=l.match(/^      - name: (.+)\$/))){ cur={name:m[1].trim()}; out.push(cur); continue; }
      if(cur && (m=l.match(/^        (if|uses|id): (.+)\$/))){ cur[m[1]]=m[2].trim(); continue; }
    }
    console.log(JSON.stringify(out));
  "
}

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
# 自动打 Tag 用的是 version.json 的版本号拼 Tag 名，下载地址必须正好指向它
TARGET_TAG="V${NAME_JSON}"
case "$URL_JSON" in
  *"/${TARGET_TAG}/"*) pass "下载地址指向自动生成的 Tag 目录 $TARGET_TAG" ;;
  *) fail "下载地址里没有 /$TARGET_TAG/ 目录，自动发布的包会 404" ;;
esac

echo
echo "=== 4. 版本自洽校验逻辑（Tag 名 / 版本号 / 下载地址）==="
# 对应工作流里的「校验版本号与下载地址」步骤
check_tag() {
  local ref="$1" expect_fail="$2"
  local target="V${NAME_JSON}"
  local rc=0
  if [[ "$ref" == refs/tags/* && "${ref#refs/tags/}" != "$target" ]]; then rc=1; fi
  case "$URL_JSON" in
    *"/${target}/"*) : ;;
    *) rc=1 ;;
  esac
  if [ "$expect_fail" = "yes" ]; then
    [ "$rc" -ne 0 ] && pass "$ref 被正确拒绝（应拒绝）" || fail "$ref 本应被拒绝却放行了"
  else
    [ "$rc" -eq 0 ] && pass "$ref 被正确接受" || fail "$ref 本应通过却被拒"
  fi
}
check_tag "refs/heads/main" no   # 提交到 main：不检查 Tag 名，只检查下载地址
check_tag "refs/tags/V6.11.0" no
check_tag "refs/tags/v6.11.0" yes   # 小写 v：App 内下载地址用的是大写 V，Tag 必须一致
check_tag "refs/tags/V6.10.1" yes   # 旧版本号：与当前版本号不符
check_tag "refs/tags/V6.12.0" yes   # 版本号超前，但 version.json 的地址里还没这个 Tag

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
echo "=== 6. 工作流里写死的签名指纹与实际产物一致 ==="
WF_FP=$(grep -oP 'EXPECTED="\K[0-9a-f]{64}' "$WF" | head -n1)
if [ -n "$WF_FP" ]; then
  [ "$WF_FP" = "6803e69fd87adfa9d71b70a8be37429ff3425cb7061dca8c27663728b95bb093" ] \
    && pass "工作流里的期望指纹正确（6803e69f…）" || fail "工作流里的期望指纹不对：$WF_FP"
else
  fail "工作流里找不到写死的签名指纹"
fi

echo
echo "=== 7. gradlew 在 Linux runner 上可直接执行 ==="
head -c 2 gradlew | od -An -tx1 | tr -d ' \n' | grep -q '^23' && pass "gradlew 以 #! 开头（无 BOM）" || fail "gradlew 不是以 #! 开头，可能有 BOM"
if grep -q $'\r' gradlew; then fail "gradlew 含 CR 换行，Linux 会报 bad interpreter"; else pass "gradlew 是 LF 换行"; fi
MODE=$(git ls-files -s gradlew | awk '{print $1}')
[ "$MODE" = "100755" ] && pass "gradlew 在 git 索引里带可执行位（100755）" || fail "gradlew 缺少可执行位（当前 $MODE）"

echo
echo "=== 8. Release 说明来自 version.json 的 updateLog ==="
LINES=$(node -e "console.log(require('${REPO_NATIVE}/version.json').updateLog)" | wc -l)
node -e "console.log(require('${REPO_NATIVE}/version.json').updateLog)" | head -2 | sed 's/^/    /'
[ "$LINES" -ge 3 ] && pass "updateLog 可转为多行 Release 说明（$LINES 行）" || fail "updateLog 行数异常"

echo
echo "=== 9. 工作流引用的 Secret 名字 ==="
for s in LOOTARCHIVE_KEYSTORE_BASE64 LOOTARCHIVE_KEYSTORE_PASSWORD LOOTARCHIVE_KEY_ALIAS LOOTARCHIVE_KEY_PASSWORD; do
  grep -q "$s" "$WF" && pass "引用了 $s" || fail "缺少 $s"
done
# 这 4 个名字必须与 app/build.gradle.kts 里 prop() 读取的名字完全一致，否则签名静默回退到 debug keystore
for s in LOOTARCHIVE_KEYSTORE_PATH LOOTARCHIVE_KEYSTORE_PASSWORD LOOTARCHIVE_KEY_ALIAS LOOTARCHIVE_KEY_PASSWORD; do
  grep -q "\"$s\"" app/build.gradle.kts && pass "app/build.gradle.kts 读取 $s" || fail "app/build.gradle.kts 未读取 $s"
done

echo
echo "=== 10. 工作流本身可被 YAML 解析 ==="
node -e "
const fs=require('fs');const t=fs.readFileSync('$WF','utf8');
// 只做基础结构检查：CI 上的 YAML 由 GitHub 解析，这里确保没有明显的制表符缩进
if(/\t/.test(t)){console.error('含制表符缩进');process.exit(1);}
if(!/^on:/m.test(t)){console.error('缺少 on:');process.exit(1);}
if(!/^jobs:/m.test(t)){console.error('缺少 jobs:');process.exit(1);}
if(!/^permissions:/m.test(t)){console.error('缺少 permissions:');process.exit(1);}
if(!/contents: write/.test(t)){console.error('缺少 contents: write（打 Tag/发 Release 需要）');process.exit(1);}
console.log('  基本结构 OK，行数='+t.split('\n').length);
" && pass "YAML 基本结构正常（无制表符、含 on:/jobs:/permissions:）" || fail "YAML 基本结构异常"

echo
echo "=== 11. Tag 决策矩阵（真跑工作流里那段 shell）==="
# 把「确定要发布的 Tag」步骤原样抽出来执行，git ls-remote 用桩替换，
# 覆盖四种情形：远端无此 Tag / Tag 指向本次提交 / Tag 指向别的提交 / 手动推 Tag。
REAL_GIT=$(command -v git)
run_tag_step() {
  local remote="$1" ref="$2" sha="$3" havekey="$4"
  printf '%s\n' "$remote" > "$WORK/remote.txt"
  mkdir -p "$WORK/bin"
  cat > "$WORK/bin/git" <<SHIM
#!/usr/bin/env bash
if [ "\$1" = "ls-remote" ]; then cat "$WORK/remote.txt"; exit 0; fi
exec "$REAL_GIT" "\$@"
SHIM
  chmod +x "$WORK/bin/git"
  : > "$WORK/out.txt"
  local script
  script=$(extract_run "确定要发布的 Tag" | sed "s|\${{ steps.ver.outputs.name }}|$NAME_JSON|g")
  PATH="$WORK/bin:$PATH" \
    GITHUB_REF="$ref" GITHUB_REF_NAME="${ref#refs/tags/}" GITHUB_SHA="$sha" \
    HAVE_KEY="$havekey" GITHUB_OUTPUT="$WORK/out.txt" \
    bash -c "$script" > "$WORK/step.log" 2>&1
  RC=$?
  STATE=$(grep -m1 '^state=' "$WORK/out.txt" | cut -d= -f2)
  PUB=$(grep -m1 '^publish=' "$WORK/out.txt" | cut -d= -f2)
}
expect_tag() {
  local desc="$1" want_state="$2" want_pub="$3"
  [ "$RC" -eq 0 ] || { fail "$desc：步骤自身报错"; sed 's/^/      /' "$WORK/step.log" | head -5; return; }
  if [ "$STATE" = "$want_state" ] && [ "$PUB" = "$want_pub" ]; then
    pass "$desc → state=$STATE publish=$PUB"
  else
    fail "$desc → 期望 state=$want_state publish=$want_pub，实际 state=$STATE publish=$PUB"
  fi
}
[ -n "$(extract_run "确定要发布的 Tag")" ] && pass "成功抽出「确定要发布的 Tag」步骤的 shell（$(extract_run "确定要发布的 Tag" | wc -l) 行）" \
  || fail "抽不出该步骤的 shell，锚点可能已变"

SHA_NOW="1111111111111111111111111111111111111111"
SHA_OLD="2222222222222222222222222222222222222222"
T="V${NAME_JSON}"

run_tag_step "" refs/heads/main "$SHA_NOW" 1
expect_tag "提交 main、远端无 $T" missing 1

run_tag_step "$SHA_NOW	refs/tags/$T" refs/heads/main "$SHA_NOW" 1
expect_tag "提交 main、$T 已指向本次提交（幂等重跑）" same 1

run_tag_step "$SHA_OLD	refs/tags/$T" refs/heads/main "$SHA_NOW" 1
expect_tag "提交 main、但没改版本号（$T 指向旧提交）" different 0

# 注解 Tag：ls-remote 会同时列出 Tag 对象与 ^{} 解引用后的提交，必须取后者
run_tag_step "$SHA_OLD	refs/tags/$T
$SHA_NOW	refs/tags/$T^{}" refs/heads/main "$SHA_NOW" 1
expect_tag "注解 Tag 取 ^{} 解引用后的提交" same 1

run_tag_step "" refs/heads/main "$SHA_NOW" 0
expect_tag "提交 main、未配签名密钥" missing 0

run_tag_step "" "refs/tags/$T" "$SHA_NOW" 1
expect_tag "手动推 Tag $T 发布" manual 1

run_tag_step "" "refs/tags/$T" "$SHA_NOW" 0
expect_tag "手动推 Tag 但没配密钥（后续步骤会硬失败）" manual 1

echo
echo "=== 12. 各步骤的触发条件 ==="
SJ=$(steps_json)
cond() { node -e "
  const s=JSON.parse(process.argv[1]).find(x=>x.name==='$1');
  if(!s){console.error('找不到步骤：$1');process.exit(2);}
  process.stdout.write(s.if||'');
" "$SJ"; }

# 自动打 Tag：只在「本次要发布」且「远端还没有这个 Tag」时执行，绝不覆盖已有 Tag
AT=$(cond "自动打 Tag")
case "$AT" in
  *"steps.tag.outputs.publish == '1'"*"steps.tag.outputs.state == 'missing'"*)
    pass "自动打 Tag 仅在 publish=1 且 state=missing 时执行（不覆盖已有 Tag）" ;;
  *) fail "自动打 Tag 的触发条件不对：$AT" ;;
esac

# Tag 必须打在签名校验之后，否则可能留下指向坏包的 Tag
LINE_SIGN=$(grep -n '校验签名指纹' "$WF" | head -n1 | cut -d: -f1)
LINE_TAG=$(grep -n 'name: 自动打 Tag' "$WF" | head -n1 | cut -d: -f1)
[ -n "$LINE_SIGN" ] && [ -n "$LINE_TAG" ] && [ "$LINE_TAG" -gt "$LINE_SIGN" ] \
  && pass "自动打 Tag 排在签名校验之后（第 $LINE_TAG 行 > 第 $LINE_SIGN 行）" \
  || fail "自动打 Tag 早于签名校验，坏包也会留下 Tag"

# 发布 Release：要 publish=1 且确实有签名（绝不用 debug keystore 发版）
REL=$(cond "发布 Release")
case "$REL" in
  *"steps.tag.outputs.publish == '1'"*"steps.sign.outputs.have == '1'"*)
    pass "发布 Release 需要 publish=1 且有签名密钥" ;;
  *) fail "发布 Release 的触发条件不对：$REL" ;;
esac

# 缺密钥时推 Tag 必须硬失败
node -e "
  const s=JSON.parse(process.argv[1]).find(x=>x.name==='校验发布用的签名密钥');
  process.exit(s && /have != '1'/.test(s.if||'') ? 0 : 1);
" "$SJ" && pass "推 Tag 但缺密钥会硬失败，不会发布错签名的包" || fail "推 Tag 缺密钥没有硬失败保护"

# 有密钥 ⇒ 提交 main 也构建正式签名的 Release；无密钥 ⇒ 退回 Debug（仅验证能否编译）
node -e "
  const s=JSON.parse(process.argv[1]);
  const rb=s.find(x=>x.name==='构建 Release APK');
  const db=s.find(x=>x.name==='构建 Debug APK（提交到 main 且未配置签名密钥时）');
  const ur=s.find(x=>x.name==='上传 Release APK');
  const ud=s.find(x=>x.name==='上传 Debug APK');
  const ok = rb && /have == '1'/.test(rb.if||'')
          && db && /have != '1'/.test(db.if||'')
          && ur && /have == '1'/.test(ur.if||'')
          && ud && /have != '1'/.test(ud.if||'');
  process.exit(ok?0:1);
" "$SJ" && pass "有密钥出 Release / 无密钥出 Debug 两条路径都闭环" || fail "Release/Debug 分支条件不完整"

echo
echo "=== 13. 触发条件包含 push 到 main ==="
node -e "
  const fs=require('fs');const t=fs.readFileSync('$WF','utf8');
  const m=t.match(/^on:[\s\S]*?^jobs:/m);
  if(!m){console.error('找不到 on: 段');process.exit(1);}
  const on=m[0];
  if(!/branches:\s*\[?\s*main/.test(on)){console.error('on: 段里没有 main 分支');process.exit(1);}
  if(!/- 'V\*'/.test(on)){console.error('on: 段里没有 V* Tag');process.exit(1);}
  console.log('  on: 段同时监听 main 分支与 V* Tag');
" && pass "push 到 main 与手动推 Tag 都会触发" || fail "触发条件不完整"

echo
echo "=== 14. versionCode 递进校验（防止静默不发更新）==="
# App 只比 versionCode 的大小来决定要不要提示更新（UpdateChecker.check(currentVersionCode)）。
# 只改版本名、忘了调大 versionCode 时，Release 照样发布成功，但所有用户都收不到更新提示。
#
# 注意：这里**不能**用「主版本×100 + 次版本×10 + 修订号」这种公式硬卡。
# 6.1.7->617、6.10.0->700、6.10.1->701、6.9.6->696 确实符合，但 6.11.0 用的是 711 而不是 710
# （历史上有手工 +1 的余量）。卡公式会把当前这棵干净的树直接判失败，而且 711 已经装到
# 用户手机上了，把它改小会造成 INSTALL_FAILED_VERSION_DOWNGRADE。
# 所以只校验真正要命的不变式：单调递增。
CUR=$(git show HEAD:version.json | tr -d '\r')
PREV=$(git show HEAD^:version.json 2>/dev/null | tr -d '\r' || true)
if [ -z "$PREV" ]; then
  pass "取不到上一个提交，跳过递进校验"
else
  PREV_NAME=$(printf '%s' "$PREV" | node -e "let s='';process.stdin.on('data',d=>s+=d).on('end',()=>console.log(JSON.parse(s).versionName))")
  PREV_CODE=$(printf '%s' "$PREV" | node -e "let s='';process.stdin.on('data',d=>s+=d).on('end',()=>console.log(JSON.parse(s).versionCode))")
  CUR_NAME="$NAME_JSON"
  [ "$CODE_JSON" -ge "$PREV_CODE" ] \
    && pass "versionCode 未回退（$PREV_CODE -> $CODE_JSON）" \
    || fail "versionCode 回退：$PREV_CODE -> $CODE_JSON（用户会收不到更新）"
  if [ "$CUR_NAME" != "$PREV_NAME" ]; then
    [ "$CODE_JSON" -gt "$PREV_CODE" ] \
      && pass "版本名 $PREV_NAME -> $CUR_NAME 时 versionCode 已调大（$PREV_CODE -> $CODE_JSON）" \
      || fail "版本名从 $PREV_NAME 改到 $CUR_NAME，但 versionCode 没变（$CODE_JSON），用户收不到更新"
  else
    pass "版本名未变（$CUR_NAME），versionCode 保持不变是允许的"
  fi
fi

# 工作流里确实做了递进校验（不是只在本脚本里核对）
extract_run "读取版本信息" | grep -q 'HEAD\^:version.json' \
  && pass "工作流内已按递进方式校验 versionCode" \
  || fail "工作流没有做 versionCode 递进校验，改漏版本号会静默不发更新提示"

# 真跑一遍该步骤的 shell：本机没有 jq，用 node 顶替（CI 上用的是真 jq，语义一致）。
cat > "$WORK/bin/jq" <<'SHIM'
#!/usr/bin/env bash
# 只支持本工作流用到的两种形态：jq -r '.field' file  与  管道 jq -r '.field'
field=""; file=""
for a in "$@"; do
  case "$a" in
    -r) ;;
    .*) field="${a#.}" ;;
    *) [ -f "$a" ] && file="$a" ;;
  esac
done
if [ -n "$file" ]; then
  # node 是 Windows 程序：必须给绝对路径，否则 require 会把它当成模块名去 node_modules 里找
  case "$file" in
    /*) native="$(cygpath -m "$file")" ;;
    *)  native="$(cygpath -m "$(pwd)/$file")" ;;
  esac
  node -e "console.log(require(process.argv[1])[process.argv[2]])" "$native" "$field"
else
  node -e "let s='';process.stdin.on('data',d=>s+=d).on('end',()=>console.log(JSON.parse(s)[process.argv[1]]))" "$field"
fi
SHIM
chmod +x "$WORK/bin/jq"

# 在一个临时仓库里造「上一个提交 / 本次提交」两个版本，真跑该步骤
VER_STEP=$(extract_run "读取版本信息")
run_ver_step() {
  local prev_name="$1" prev_code="$2" cur_name="$3" cur_code="$4"
  local d="$WORK/vercase"
  rm -rf "$d"; mkdir -p "$d/app"
  (
    cd "$d" || exit 1
    git init -q .
    cat > app/build.gradle.kts <<EOF
android {
    defaultConfig {
        versionCode = $prev_code
        versionName = "$prev_name"
    }
}
EOF
    cat > version.json <<EOF
{
  "versionName": "$prev_name",
  "versionCode": $prev_code,
  "apkDownloadUrl": "https://github.com/nanji0710/LootArchive/releases/download/V$prev_name/LootArchive-release-v$prev_name.apk"
}
EOF
    git add -A >/dev/null 2>&1
    git -c user.email=a@b -c user.name=a commit -qm prev >/dev/null 2>&1
    cat > app/build.gradle.kts <<EOF
android {
    defaultConfig {
        versionCode = $cur_code
        versionName = "$cur_name"
    }
}
EOF
    cat > version.json <<EOF
{
  "versionName": "$cur_name",
  "versionCode": $cur_code,
  "apkDownloadUrl": "https://github.com/nanji0710/LootArchive/releases/download/V$cur_name/LootArchive-release-v$cur_name.apk"
}
EOF
    git add -A >/dev/null 2>&1
    git -c user.email=a@b -c user.name=a commit -qm cur >/dev/null 2>&1
  )
  : > "$WORK/verout.txt"
  ( cd "$d" && PATH="$WORK/bin:$PATH" GITHUB_OUTPUT="$WORK/verout.txt" bash -c "$VER_STEP" ) > "$WORK/ver.log" 2>&1
  VER_RC=$?
}
expect_ver() {
  local desc="$1" want_rc="$2"
  if [ "$want_rc" = "0" ]; then
    [ "$VER_RC" -eq 0 ] && pass "$desc" \
      || { fail "$desc（预期通过却退出 $VER_RC）"; sed 's/^/      /' "$WORK/ver.log" | tail -4; }
  else
    [ "$VER_RC" -ne 0 ] && pass "$desc" \
      || fail "$desc（预期被拦下却通过了）"
    grep -q '::error::' "$WORK/ver.log" \
      && pass "$desc：给出了 ::error:: 说明" \
      || fail "$desc：被拦下但没给出 ::error:: 说明"
  fi
}
run_ver_step 6.11.0 711 6.12.0 712
expect_ver "版本名与版本号一起改大 → 放行" 0
[ "$(grep -m1 '^name=' "$WORK/verout.txt" | cut -d= -f2)" = "6.12.0" ] \
  && pass "输出 name=6.12.0" || fail "输出的 name 不对"
[ "$(grep -m1 '^code=' "$WORK/verout.txt" | cut -d= -f2)" = "712" ] \
  && pass "输出 code=712" || fail "输出的 code 不对"

run_ver_step 6.11.0 711 6.12.0 711
expect_ver "只改版本名、忘了调大 versionCode → 拦下（否则用户收不到更新）" 1

run_ver_step 6.11.0 711 6.11.0 711
expect_ver "版本号完全没改（重复推送）→ 放行，交给 Tag 状态判定" 0

run_ver_step 6.11.0 711 6.10.1 701
expect_ver "versionCode 回退 → 拦下（会造成降级，用户收不到更新）" 1

run_ver_step 6.11.0 711 6.11.0 712
expect_ver "版本名不变但只调大 versionCode → 放行" 0

# 递进校验依赖上一个提交，checkout 必须取够历史，否则那道校验会静默失效
awk '/- name: 检出代码/{f=1} f&&/fetch-depth:/{print;exit} f&&/- name: 安装/{exit}' "$WF" | grep -q 'fetch-depth: *2' \
  && pass "checkout 取了 2 个提交，递进校验能读到上一个 version.json" \
  || fail "checkout 没取够历史，versionCode 递进校验会静默失效"

# 历史里的真实版本号必须仍被放行（防止这道校验误伤正常发版）
run_ver_step 6.10.0 700 6.10.1 701
expect_ver "历史版本 6.10.0→6.10.1 仍被放行（校验没误伤）" 0

echo
echo "=== 15. 并发与 lint 绕过 ==="
# 连推两次时若两个运行并行，会各自认为「Tag 还不存在」而抢同一个 Tag，
# 后一个必然失败并报出无意义的红灯，所以必须有 concurrency。
node -e "
  const fs=require('fs');const t=fs.readFileSync('$WF','utf8');
  if(!/^concurrency:/m.test(t)){console.error('缺少 concurrency:（连推两次会抢 Tag）');process.exit(1);}
  if(!/group:\s*\S/.test(t)){console.error('concurrency 缺少 group');process.exit(1);}
  console.log('  已串行化同一分支的构建');
" && pass "有 concurrency，连推两次不会抢同一个 Tag" || fail "缺少 concurrency 保护"

# lint 的 NullSafeMutableLiveData 检测器在本项目上抛 IncompatibleClassChangeError
# （lint 自身缺陷），Release 构建必须显式跳过，否则 CI 会在 lint 阶段崩溃。
extract_run "构建 Release APK" | grep -q -- '-x lintVitalRelease' \
  && pass "Release 构建跳过了会崩溃的 lintVitalRelease" \
  || fail "Release 构建没有 -x lintVitalRelease，CI 会在 lint 阶段崩溃"

# Debug 打包不能顺手把 lint 也绕过，否则本地/CI 都失去静态检查
if extract_run "构建 Release APK" | grep -q -- 'assembleRelease'; then
  pass "Release 构建用的是 assembleRelease"
else
  fail "Release 构建命令不是 assembleRelease"
fi

echo
if [ "$FAIL" -eq 0 ]; then echo "==> 全部通过"; else echo "==> 有 $FAIL 项失败"; fi
exit "$FAIL"
