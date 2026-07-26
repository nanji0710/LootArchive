package com.nanji.lootarchive.ui.detail;

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
public final class DetailViewModel_Factory implements Factory<DetailViewModel> {
  private final Provider<ItemRepository> itemRepositoryProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  public DetailViewModel_Factory(Provider<ItemRepository> itemRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    this.itemRepositoryProvider = itemRepositoryProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  @Override
  public DetailViewModel get() {
    return newInstance(itemRepositoryProvider.get(), settingsRepositoryProvider.get());
  }

  public static DetailViewModel_Factory create(Provider<ItemRepository> itemRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    return new DetailViewModel_Factory(itemRepositoryProvider, settingsRepositoryProvider);
  }

  public static DetailViewModel newInstance(ItemRepository itemRepository,
      SettingsRepository settingsRepository) {
    return new DetailViewModel(itemRepository, settingsRepository);
  }
}
