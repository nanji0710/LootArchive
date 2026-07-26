package com.nanji.lootarchive.data.repository;

import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class SettingsRepository_Factory implements Factory<SettingsRepository> {
  private final Provider<DataStore<Preferences>> dataStoreProvider;

  public SettingsRepository_Factory(Provider<DataStore<Preferences>> dataStoreProvider) {
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public SettingsRepository get() {
    return newInstance(dataStoreProvider.get());
  }

  public static SettingsRepository_Factory create(
      Provider<DataStore<Preferences>> dataStoreProvider) {
    return new SettingsRepository_Factory(dataStoreProvider);
  }

  public static SettingsRepository newInstance(DataStore<Preferences> dataStore) {
    return new SettingsRepository(dataStore);
  }
}
