package com.nanji.lootarchive.ui.settings;

import android.app.Application;
import com.nanji.lootarchive.data.repository.ItemRepository;
import com.nanji.lootarchive.data.repository.SettingsRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<Application> appProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<ItemRepository> itemRepositoryProvider;

  public SettingsViewModel_Factory(Provider<Application> appProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<ItemRepository> itemRepositoryProvider) {
    this.appProvider = appProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.itemRepositoryProvider = itemRepositoryProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(appProvider.get(), settingsRepositoryProvider.get(), itemRepositoryProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<Application> appProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<ItemRepository> itemRepositoryProvider) {
    return new SettingsViewModel_Factory(appProvider, settingsRepositoryProvider, itemRepositoryProvider);
  }

  public static SettingsViewModel newInstance(Application app,
      SettingsRepository settingsRepository, ItemRepository itemRepository) {
    return new SettingsViewModel(app, settingsRepository, itemRepository);
  }
}
