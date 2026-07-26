package com.nanji.lootarchive.ui.additem;

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
public final class AddItemViewModel_Factory implements Factory<AddItemViewModel> {
  private final Provider<ItemRepository> itemRepositoryProvider;

  private final Provider<CategoryRepository> categoryRepositoryProvider;

  public AddItemViewModel_Factory(Provider<ItemRepository> itemRepositoryProvider,
      Provider<CategoryRepository> categoryRepositoryProvider) {
    this.itemRepositoryProvider = itemRepositoryProvider;
    this.categoryRepositoryProvider = categoryRepositoryProvider;
  }

  @Override
  public AddItemViewModel get() {
    return newInstance(itemRepositoryProvider.get(), categoryRepositoryProvider.get());
  }

  public static AddItemViewModel_Factory create(Provider<ItemRepository> itemRepositoryProvider,
      Provider<CategoryRepository> categoryRepositoryProvider) {
    return new AddItemViewModel_Factory(itemRepositoryProvider, categoryRepositoryProvider);
  }

  public static AddItemViewModel newInstance(ItemRepository itemRepository,
      CategoryRepository categoryRepository) {
    return new AddItemViewModel(itemRepository, categoryRepository);
  }
}
