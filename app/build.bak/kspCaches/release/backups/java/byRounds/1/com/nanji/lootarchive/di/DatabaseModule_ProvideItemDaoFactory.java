package com.nanji.lootarchive.di;

import com.nanji.lootarchive.data.local.dao.ItemDao;
import com.nanji.lootarchive.data.local.database.AppDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideItemDaoFactory implements Factory<ItemDao> {
  private final Provider<AppDatabase> dbProvider;

  public DatabaseModule_ProvideItemDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public ItemDao get() {
    return provideItemDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideItemDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvideItemDaoFactory(dbProvider);
  }

  public static ItemDao provideItemDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideItemDao(db));
  }
}
