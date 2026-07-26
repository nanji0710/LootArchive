package com.nanji.lootarchive.data.repository;

import android.content.Context;
import com.nanji.lootarchive.data.local.dao.BackupRecordDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class BackupRepository_Factory implements Factory<BackupRepository> {
  private final Provider<Context> contextProvider;

  private final Provider<BackupRecordDao> backupRecordDaoProvider;

  public BackupRepository_Factory(Provider<Context> contextProvider,
      Provider<BackupRecordDao> backupRecordDaoProvider) {
    this.contextProvider = contextProvider;
    this.backupRecordDaoProvider = backupRecordDaoProvider;
  }

  @Override
  public BackupRepository get() {
    return newInstance(contextProvider.get(), backupRecordDaoProvider.get());
  }

  public static BackupRepository_Factory create(Provider<Context> contextProvider,
      Provider<BackupRecordDao> backupRecordDaoProvider) {
    return new BackupRepository_Factory(contextProvider, backupRecordDaoProvider);
  }

  public static BackupRepository newInstance(Context context, BackupRecordDao backupRecordDao) {
    return new BackupRepository(context, backupRecordDao);
  }
}
