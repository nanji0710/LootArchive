package com.nanji.lootarchive.data.repository;

import com.nanji.lootarchive.data.local.dao.CategoryDao;
import com.nanji.lootarchive.data.local.dao.ItemDao;
import com.nanji.lootarchive.data.local.dao.ItemPhotoDao;
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
public final class ItemRepository_Factory implements Factory<ItemRepository> {
  private final Provider<ItemDao> itemDaoProvider;

  private final Provider<CategoryDao> categoryDaoProvider;

  private final Provider<ItemPhotoDao> itemPhotoDaoProvider;

  public ItemRepository_Factory(Provider<ItemDao> itemDaoProvider,
      Provider<CategoryDao> categoryDaoProvider, Provider<ItemPhotoDao> itemPhotoDaoProvider) {
    this.itemDaoProvider = itemDaoProvider;
    this.categoryDaoProvider = categoryDaoProvider;
    this.itemPhotoDaoProvider = itemPhotoDaoProvider;
  }

  @Override
  public ItemRepository get() {
    return newInstance(itemDaoProvider.get(), categoryDaoProvider.get(), itemPhotoDaoProvider.get());
  }

  public static ItemRepository_Factory create(Provider<ItemDao> itemDaoProvider,
      Provider<CategoryDao> categoryDaoProvider, Provider<ItemPhotoDao> itemPhotoDaoProvider) {
    return new ItemRepository_Factory(itemDaoProvider, categoryDaoProvider, itemPhotoDaoProvider);
  }

  public static ItemRepository newInstance(ItemDao itemDao, CategoryDao categoryDao,
      ItemPhotoDao itemPhotoDao) {
    return new ItemRepository(itemDao, categoryDao, itemPhotoDao);
  }
}
