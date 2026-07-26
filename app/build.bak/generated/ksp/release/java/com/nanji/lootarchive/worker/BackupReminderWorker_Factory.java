package com.nanji.lootarchive.worker;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.nanji.lootarchive.data.repository.SettingsRepository;
import dagger.internal.DaggerGenerated;
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
public final class BackupReminderWorker_Factory {
  private final Provider<SettingsRepository> settingsRepositoryProvider;

  public BackupReminderWorker_Factory(Provider<SettingsRepository> settingsRepositoryProvider) {
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  public BackupReminderWorker get(Context context, WorkerParameters workerParams) {
    return newInstance(context, workerParams, settingsRepositoryProvider.get());
  }

  public static BackupReminderWorker_Factory create(
      Provider<SettingsRepository> settingsRepositoryProvider) {
    return new BackupReminderWorker_Factory(settingsRepositoryProvider);
  }

  public static BackupReminderWorker newInstance(Context context, WorkerParameters workerParams,
      SettingsRepository settingsRepository) {
    return new BackupReminderWorker(context, workerParams, settingsRepository);
  }
}
