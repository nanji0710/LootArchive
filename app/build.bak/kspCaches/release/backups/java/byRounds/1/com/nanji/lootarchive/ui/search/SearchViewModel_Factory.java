package com.nanji.lootarchive.ui.search;

import com.nanji.lootarchive.data.repository.CategoryRepository;
import com.nanji.lootarchive.data.repository.ItemRepository;
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
public final class SearchViewModel_Factory implements Factory<SearchViewModel> {
  private final Provider<ItemRepository> itemRepositoryProvider;

  private final Provider<CategoryRepository> categoryRepositoryProvider;

  public SearchViewModel_Factory(Provider<ItemRepository> itemRepositoryProvider,
      Provider<CategoryRepository> categoryRepositoryProvider) {
    this.itemRepositoryProvider = itemRepositoryProvider;
    this.categoryRepositoryProvider = categoryRepositoryProvider;
  }

  @Override
  public SearchViewModel get() {
    return newInstance(itemRepositoryProvider.get(), categoryRepositoryProvider.get());
  }

  public static SearchViewModel_Factory create(Provider<ItemRepository> itemRepositoryProvider,
      Provider<CategoryRepository> categoryRepositoryProvider) {
    return new SearchViewModel_Factory(itemRepositoryProvider, categoryRepositoryProvider);
  }

  public static SearchViewModel newInstance(ItemRepository itemRepository,
      CategoryRepository categoryRepository) {
    return new SearchViewModel(itemRepository, categoryRepository);
  }
}
