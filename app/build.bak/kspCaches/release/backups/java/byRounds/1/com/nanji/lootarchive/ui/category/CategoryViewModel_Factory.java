package com.nanji.lootarchive.ui.category;

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
public final class CategoryViewModel_Factory implements Factory<CategoryViewModel> {
  private final Provider<CategoryRepository> categoryRepositoryProvider;

  private final Provider<ItemRepository> itemRepositoryProvider;

  public CategoryViewModel_Factory(Provider<CategoryRepository> categoryRepositoryProvider,
      Provider<ItemRepository> itemRepositoryProvider) {
    this.categoryRepositoryProvider = categoryRepositoryProvider;
    this.itemRepositoryProvider = itemRepositoryProvider;
  }

  @Override
  public CategoryViewModel get() {
    return newInstance(categoryRepositoryProvider.get(), itemRepositoryProvider.get());
  }

  public static CategoryViewModel_Factory create(
      Provider<CategoryRepository> categoryRepositoryProvider,
      Provider<ItemRepository> itemRepositoryProvider) {
    return new CategoryViewModel_Factory(categoryRepositoryProvider, itemRepositoryProvider);
  }

  public static CategoryViewModel newInstance(CategoryRepository categoryRepository,
      ItemRepository itemRepository) {
    return new CategoryViewModel(categoryRepository, itemRepository);
  }
}
