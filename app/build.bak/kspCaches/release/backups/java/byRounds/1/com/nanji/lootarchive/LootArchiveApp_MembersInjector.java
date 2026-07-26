package com.nanji.lootarchive;

import androidx.hilt.work.HiltWorkerFactory;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class LootArchiveApp_MembersInjector implements MembersInjector<LootArchiveApp> {
  private final Provider<HiltWorkerFactory> workerFactoryProvider;

  public LootArchiveApp_MembersInjector(Provider<HiltWorkerFactory> workerFactoryProvider) {
    this.workerFactoryProvider = workerFactoryProvider;
  }

  public static MembersInjector<LootArchiveApp> create(
      Provider<HiltWorkerFactory> workerFactoryProvider) {
    return new LootArchiveApp_MembersInjector(workerFactoryProvider);
  }

  @Override
  public void injectMembers(LootArchiveApp instance) {
    injectWorkerFactory(instance, workerFactoryProvider.get());
  }

  @InjectedFieldSignature("com.nanji.lootarchive.LootArchiveApp.workerFactory")
  public static void injectWorkerFactory(LootArchiveApp instance, HiltWorkerFactory workerFactory) {
    instance.workerFactory = workerFactory;
  }
}
