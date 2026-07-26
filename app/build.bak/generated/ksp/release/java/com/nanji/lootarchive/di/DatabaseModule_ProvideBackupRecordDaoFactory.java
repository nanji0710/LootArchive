package com.nanji.lootarchive.di;

import com.nanji.lootarchive.data.local.dao.BackupRecordDao;
import com.nanji.lootarchive.data.local.database.AppDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
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
public final class DatabaseModule_ProvideBackupRecordDaoFactory implements Factory<BackupRecordDao> {
  private final Provider<AppDatabase> dbProvider;

  public DatabaseModule_ProvideBackupRecordDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public BackupRecordDao get() {
    return provideBackupRecordDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideBackupRecordDaoFactory create(
      Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvideBackupRecordDaoFactory(dbProvider);
  }

  public static BackupRecordDao provideBackupRecordDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideBackupRecordDao(db));
  }
}
