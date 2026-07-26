package com.nanji.lootarchive.di;

import com.nanji.lootarchive.data.local.dao.ItemPhotoDao;
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
public final class DatabaseModule_ProvideItemPhotoDaoFactory implements Factory<ItemPhotoDao> {
  private final Provider<AppDatabase> dbProvider;

  public DatabaseModule_ProvideItemPhotoDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public ItemPhotoDao get() {
    return provideItemPhotoDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideItemPhotoDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvideItemPhotoDaoFactory(dbProvider);
  }

  public static ItemPhotoDao provideItemPhotoDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideItemPhotoDao(db));
  }
}
