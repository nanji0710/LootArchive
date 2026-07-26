package com.nanji.lootarchive.worker;

import android.content.Context;
import androidx.work.WorkerParameters;
import dagger.internal.DaggerGenerated;
import dagger.internal.InstanceFactory;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class WarrantyCheckWorker_AssistedFactory_Impl implements WarrantyCheckWorker_AssistedFactory {
  private final WarrantyCheckWorker_Factory delegateFactory;

  WarrantyCheckWorker_AssistedFactory_Impl(WarrantyCheckWorker_Factory delegateFactory) {
    this.delegateFactory = delegateFactory;
  }

  @Override
  public WarrantyCheckWorker create(Context p0, WorkerParameters p1) {
    return delegateFactory.get(p0, p1);
  }

  public static Provider<WarrantyCheckWorker_AssistedFactory> create(
      WarrantyCheckWorker_Factory delegateFactory) {
    return InstanceFactory.create(new WarrantyCheckWorker_AssistedFactory_Impl(delegateFactory));
  }

  public static dagger.internal.Provider<WarrantyCheckWorker_AssistedFactory> createFactoryProvider(
      WarrantyCheckWorker_Factory delegateFactory) {
    return InstanceFactory.create(new WarrantyCheckWorker_AssistedFactory_Impl(delegateFactory));
  }
}
