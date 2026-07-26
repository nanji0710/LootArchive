package com.nanji.lootarchive.ui.statistics;

import com.nanji.lootarchive.data.repository.CategoryRepository;
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
public final class StatisticsViewModel_Factory implements Factory<StatisticsViewModel> {
  private final Provider<ItemRepository> itemRepositoryProvider;

  private final Provider<CategoryRepository> categoryRepositoryProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  public StatisticsViewModel_Factory(Provider<ItemRepository> itemRepositoryProvider,
      Provider<CategoryRepository> categoryRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    this.itemRepositoryProvider = itemRepositoryProvider;
    this.categoryRepositoryProvider = categoryRepositoryProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  @Override
  public StatisticsViewModel get() {
    return newInstance(itemRepositoryProvider.get(), categoryRepositoryProvider.get(), settingsRepositoryProvider.get());
  }

  public static StatisticsViewModel_Factory create(Provider<ItemRepository> itemRepositoryProvider,
      Provider<CategoryRepository> categoryRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    return new StatisticsViewModel_Factory(itemRepositoryProvider, categoryRepositoryProvider, settingsRepositoryProvider);
  }

  public static StatisticsViewModel newInstance(ItemRepository itemRepository,
      CategoryRepository categoryRepository, SettingsRepository settingsRepository) {
    return new StatisticsViewModel(itemRepository, categoryRepository, settingsRepository);
  }
}
