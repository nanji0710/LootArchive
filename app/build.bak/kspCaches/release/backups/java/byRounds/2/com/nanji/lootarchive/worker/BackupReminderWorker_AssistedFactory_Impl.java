package com.nanji.lootarchive.worker;

import android.content.Context;
import androidx.work.WorkerParameters;
import dagger.internal.DaggerGenerated;
import dagger.internal.InstanceFactory;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class BackupReminderWorker_AssistedFactory_Impl implements BackupReminderWorker_AssistedFactory {
  private final BackupReminderWorker_Factory delegateFactory;

  BackupReminderWorker_AssistedFactory_Impl(BackupReminderWorker_Factory delegateFactory) {
    this.delegateFactory = delegateFactory;
  }

  @Override
  public BackupReminderWorker create(Context p0, WorkerParameters p1) {
    return delegateFactory.get(p0, p1);
  }

  public static Provider<BackupReminderWorker_AssistedFactory> create(
      BackupReminderWorker_Factory delegateFactory) {
    return InstanceFactory.create(new BackupReminderWorker_AssistedFactory_Impl(delegateFactory));
  }

  public static dagger.internal.Provider<BackupReminderWorker_AssistedFactory> createFactoryProvider(
      BackupReminderWorker_Factory delegateFactory) {
    return InstanceFactory.create(new BackupReminderWorker_AssistedFactory_Impl(delegateFactory));
  }
}
