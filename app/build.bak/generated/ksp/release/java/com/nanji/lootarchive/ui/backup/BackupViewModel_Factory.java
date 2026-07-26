package com.nanji.lootarchive.ui.backup;

import android.content.Context;
import com.nanji.lootarchive.data.repository.BackupRepository;
import com.nanji.lootarchive.data.repository.CategoryRepository;
import com.nanji.lootarchive.data.repository.ItemRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class BackupViewModel_Factory implements Factory<BackupViewModel> {
  private final Provider<Context> contextProvider;

  private final Provider<BackupRepository> backupRepositoryProvider;

  private final Provider<ItemRepository> itemRepositoryProvider;

  private final Provider<CategoryRepository> categoryRepositoryProvider;

  public BackupViewModel_Factory(Provider<Context> contextProvider,
      Provider<BackupRepository> backupRepositoryProvider,
      Provider<ItemRepository> itemRepositoryProvider,
      Provider<CategoryRepository> categoryRepositoryProvider) {
    this.contextProvider = contextProvider;
    this.backupRepositoryProvider = backupRepositoryProvider;
    this.itemRepositoryProvider = itemRepositoryProvider;
    this.categoryRepositoryProvider = categoryRepositoryProvider;
  }

  @Override
  public BackupViewModel get() {
    return newInstance(contextProvider.get(), backupRepositoryProvider.get(), itemRepositoryProvider.get(), categoryRepositoryProvider.get());
  }

  public static BackupViewModel_Factory create(Provider<Context> contextProvider,
      Provider<BackupRepository> backupRepositoryProvider,
      Provider<ItemRepository> itemRepositoryProvider,
      Provider<CategoryRepository> categoryRepositoryProvider) {
    return new BackupViewModel_Factory(contextProvider, backupRepositoryProvider, itemRepositoryProvider, categoryRepositoryProvider);
  }

  public static BackupViewModel newInstance(Context context, BackupRepository backupRepository,
      ItemRepository itemRepository, CategoryRepository categoryRepository) {
    return new BackupViewModel(context, backupRepository, itemRepository, categoryRepository);
  }
}
