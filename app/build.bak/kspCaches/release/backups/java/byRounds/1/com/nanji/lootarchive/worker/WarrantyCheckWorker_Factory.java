package com.nanji.lootarchive.worker;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.nanji.lootarchive.data.repository.ItemRepository;
import com.nanji.lootarchive.data.repository.SettingsRepository;
import dagger.internal.DaggerGenerated;
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
public final class WarrantyCheckWorker_Factory {
  private final Provider<ItemRepository> itemRepositoryProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  public WarrantyCheckWorker_Factory(Provider<ItemRepository> itemRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    this.itemRepositoryProvider = itemRepositoryProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  public WarrantyCheckWorker get(Context context, WorkerParameters workerParams) {
    return newInstance(context, workerParams, itemRepositoryProvider.get(), settingsRepositoryProvider.get());
  }

  public static WarrantyCheckWorker_Factory create(Provider<ItemRepository> itemRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    return new WarrantyCheckWorker_Factory(itemRepositoryProvider, settingsRepositoryProvider);
  }

  public static WarrantyCheckWorker newInstance(Context context, WorkerParameters workerParams,
      ItemRepository itemRepository, SettingsRepository settingsRepository) {
    return new WarrantyCheckWorker(context, workerParams, itemRepository, settingsRepository);
  }
}
