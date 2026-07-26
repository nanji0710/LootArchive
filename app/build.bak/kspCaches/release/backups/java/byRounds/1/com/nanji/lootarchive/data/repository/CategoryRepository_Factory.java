package com.nanji.lootarchive.data.repository;

import com.nanji.lootarchive.data.local.dao.CategoryDao;
import com.nanji.lootarchive.data.local.dao.ItemDao;
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
public final class CategoryRepository_Factory implements Factory<CategoryRepository> {
  private final Provider<CategoryDao> categoryDaoProvider;

  private final Provider<ItemDao> itemDaoProvider;

  public CategoryRepository_Factory(Provider<CategoryDao> categoryDaoProvider,
      Provider<ItemDao> itemDaoProvider) {
    this.categoryDaoProvider = categoryDaoProvider;
    this.itemDaoProvider = itemDaoProvider;
  }

  @Override
  public CategoryRepository get() {
    return newInstance(categoryDaoProvider.get(), itemDaoProvider.get());
  }

  public static CategoryRepository_Factory create(Provider<CategoryDao> categoryDaoProvider,
      Provider<ItemDao> itemDaoProvider) {
    return new CategoryRepository_Factory(categoryDaoProvider, itemDaoProvider);
  }

  public static CategoryRepository newInstance(CategoryDao categoryDao, ItemDao itemDao) {
    return new CategoryRepository(categoryDao, itemDao);
  }
}
