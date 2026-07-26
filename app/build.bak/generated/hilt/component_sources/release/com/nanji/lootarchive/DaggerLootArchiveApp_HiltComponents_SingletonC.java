package com.nanji.lootarchive;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.view.View;
import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import androidx.fragment.app.Fragment;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.hilt.work.WorkerAssistedFactory;
import androidx.hilt.work.WorkerFactoryModule_ProvideFactoryFactory;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;
import com.nanji.lootarchive.data.local.dao.BackupRecordDao;
import com.nanji.lootarchive.data.local.dao.CategoryDao;
import com.nanji.lootarchive.data.local.dao.ItemDao;
import com.nanji.lootarchive.data.local.dao.ItemPhotoDao;
import com.nanji.lootarchive.data.local.database.AppDatabase;
import com.nanji.lootarchive.data.repository.BackupRepository;
import com.nanji.lootarchive.data.repository.CategoryRepository;
import com.nanji.lootarchive.data.repository.ItemRepository;
import com.nanji.lootarchive.data.repository.SettingsRepository;
import com.nanji.lootarchive.di.AppModule_ProvideDataStoreFactory;
import com.nanji.lootarchive.di.DatabaseModule_ProvideBackupRecordDaoFactory;
import com.nanji.lootarchive.di.DatabaseModule_ProvideCategoryDaoFactory;
import com.nanji.lootarchive.di.DatabaseModule_ProvideDatabaseFactory;
import com.nanji.lootarchive.di.DatabaseModule_ProvideItemDaoFactory;
import com.nanji.lootarchive.di.DatabaseModule_ProvideItemPhotoDaoFactory;
import com.nanji.lootarchive.ui.additem.AddItemViewModel;
import com.nanji.lootarchive.ui.additem.AddItemViewModel_HiltModules;
import com.nanji.lootarchive.ui.additem.AddItemViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.nanji.lootarchive.ui.additem.AddItemViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.nanji.lootarchive.ui.backup.BackupViewModel;
import com.nanji.lootarchive.ui.backup.BackupViewModel_HiltModules;
import com.nanji.lootarchive.ui.backup.BackupViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.nanji.lootarchive.ui.backup.BackupViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.nanji.lootarchive.ui.category.CategoryViewModel;
import com.nanji.lootarchive.ui.category.CategoryViewModel_HiltModules;
import com.nanji.lootarchive.ui.category.CategoryViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.nanji.lootarchive.ui.category.CategoryViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.nanji.lootarchive.ui.component.CategoryDrawerViewModel;
import com.nanji.lootarchive.ui.component.CategoryDrawerViewModel_HiltModules;
import com.nanji.lootarchive.ui.component.CategoryDrawerViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.nanji.lootarchive.ui.component.CategoryDrawerViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.nanji.lootarchive.ui.detail.DetailViewModel;
import com.nanji.lootarchive.ui.detail.DetailViewModel_HiltModules;
import com.nanji.lootarchive.ui.detail.DetailViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.nanji.lootarchive.ui.detail.DetailViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.nanji.lootarchive.ui.home.HomeViewModel;
import com.nanji.lootarchive.ui.home.HomeViewModel_HiltModules;
import com.nanji.lootarchive.ui.home.HomeViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.nanji.lootarchive.ui.home.HomeViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.nanji.lootarchive.ui.search.SearchViewModel;
import com.nanji.lootarchive.ui.search.SearchViewModel_HiltModules;
import com.nanji.lootarchive.ui.search.SearchViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.nanji.lootarchive.ui.search.SearchViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.nanji.lootarchive.ui.settings.SettingsViewModel;
import com.nanji.lootarchive.ui.settings.SettingsViewModel_HiltModules;
import com.nanji.lootarchive.ui.settings.SettingsViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.nanji.lootarchive.ui.settings.SettingsViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.nanji.lootarchive.ui.statistics.StatisticsViewModel;
import com.nanji.lootarchive.ui.statistics.StatisticsViewModel_HiltModules;
import com.nanji.lootarchive.ui.statistics.StatisticsViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.nanji.lootarchive.ui.statistics.StatisticsViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.nanji.lootarchive.worker.BackupReminderWorker;
import com.nanji.lootarchive.worker.BackupReminderWorker_AssistedFactory;
import com.nanji.lootarchive.worker.WarrantyCheckWorker;
import com.nanji.lootarchive.worker.WarrantyCheckWorker_AssistedFactory;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideApplicationFactory;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.MapBuilder;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.SingleCheck;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

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
public final class DaggerLootArchiveApp_HiltComponents_SingletonC {
  private DaggerLootArchiveApp_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public LootArchiveApp_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements LootArchiveApp_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public LootArchiveApp_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements LootArchiveApp_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public LootArchiveApp_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements LootArchiveApp_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public LootArchiveApp_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements LootArchiveApp_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public LootArchiveApp_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements LootArchiveApp_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public LootArchiveApp_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements LootArchiveApp_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public LootArchiveApp_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements LootArchiveApp_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public LootArchiveApp_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends LootArchiveApp_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends LootArchiveApp_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends LootArchiveApp_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends LootArchiveApp_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
      injectMainActivity2(mainActivity);
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(MapBuilder.<String, Boolean>newMapBuilder(9).put(AddItemViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, AddItemViewModel_HiltModules.KeyModule.provide()).put(BackupViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, BackupViewModel_HiltModules.KeyModule.provide()).put(CategoryDrawerViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, CategoryDrawerViewModel_HiltModules.KeyModule.provide()).put(CategoryViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, CategoryViewModel_HiltModules.KeyModule.provide()).put(DetailViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, DetailViewModel_HiltModules.KeyModule.provide()).put(HomeViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, HomeViewModel_HiltModules.KeyModule.provide()).put(SearchViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, SearchViewModel_HiltModules.KeyModule.provide()).put(SettingsViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, SettingsViewModel_HiltModules.KeyModule.provide()).put(StatisticsViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, StatisticsViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    private MainActivity injectMainActivity2(MainActivity instance) {
      MainActivity_MembersInjector.injectSettingsRepository(instance, singletonCImpl.settingsRepositoryProvider.get());
      return instance;
    }
  }

  private static final class ViewModelCImpl extends LootArchiveApp_HiltComponents.ViewModelC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<AddItemViewModel> addItemViewModelProvider;

    private Provider<BackupViewModel> backupViewModelProvider;

    private Provider<CategoryDrawerViewModel> categoryDrawerViewModelProvider;

    private Provider<CategoryViewModel> categoryViewModelProvider;

    private Provider<DetailViewModel> detailViewModelProvider;

    private Provider<HomeViewModel> homeViewModelProvider;

    private Provider<SearchViewModel> searchViewModelProvider;

    private Provider<SettingsViewModel> settingsViewModelProvider;

    private Provider<StatisticsViewModel> statisticsViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;

      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.addItemViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.backupViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.categoryDrawerViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.categoryViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.detailViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.homeViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
      this.searchViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 6);
      this.settingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 7);
      this.statisticsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 8);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(MapBuilder.<String, javax.inject.Provider<ViewModel>>newMapBuilder(9).put(AddItemViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) addItemViewModelProvider)).put(BackupViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) backupViewModelProvider)).put(CategoryDrawerViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) categoryDrawerViewModelProvider)).put(CategoryViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) categoryViewModelProvider)).put(DetailViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) detailViewModelProvider)).put(HomeViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) homeViewModelProvider)).put(SearchViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) searchViewModelProvider)).put(SettingsViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) settingsViewModelProvider)).put(StatisticsViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) statisticsViewModelProvider)).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return Collections.<Class<?>, Object>emptyMap();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.nanji.lootarchive.ui.additem.AddItemViewModel 
          return (T) new AddItemViewModel(singletonCImpl.itemRepositoryProvider.get(), singletonCImpl.categoryRepositoryProvider.get());

          case 1: // com.nanji.lootarchive.ui.backup.BackupViewModel 
          return (T) new BackupViewModel(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.backupRepositoryProvider.get(), singletonCImpl.itemRepositoryProvider.get(), singletonCImpl.categoryRepositoryProvider.get());

          case 2: // com.nanji.lootarchive.ui.component.CategoryDrawerViewModel 
          return (T) new CategoryDrawerViewModel(singletonCImpl.categoryRepositoryProvider.get(), singletonCImpl.itemRepositoryProvider.get());

          case 3: // com.nanji.lootarchive.ui.category.CategoryViewModel 
          return (T) new CategoryViewModel(singletonCImpl.categoryRepositoryProvider.get(), singletonCImpl.itemRepositoryProvider.get());

          case 4: // com.nanji.lootarchive.ui.detail.DetailViewModel 
          return (T) new DetailViewModel(singletonCImpl.itemRepositoryProvider.get(), singletonCImpl.settingsRepositoryProvider.get());

          case 5: // com.nanji.lootarchive.ui.home.HomeViewModel 
          return (T) new HomeViewModel(singletonCImpl.itemRepositoryProvider.get(), singletonCImpl.settingsRepositoryProvider.get());

          case 6: // com.nanji.lootarchive.ui.search.SearchViewModel 
          return (T) new SearchViewModel(singletonCImpl.itemRepositoryProvider.get(), singletonCImpl.categoryRepositoryProvider.get());

          case 7: // com.nanji.lootarchive.ui.settings.SettingsViewModel 
          return (T) new SettingsViewModel(ApplicationContextModule_ProvideApplicationFactory.provideApplication(singletonCImpl.applicationContextModule), singletonCImpl.settingsRepositoryProvider.get(), singletonCImpl.itemRepositoryProvider.get());

          case 8: // com.nanji.lootarchive.ui.statistics.StatisticsViewModel 
          return (T) new StatisticsViewModel(singletonCImpl.itemRepositoryProvider.get(), singletonCImpl.categoryRepositoryProvider.get(), singletonCImpl.settingsRepositoryProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends LootArchiveApp_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends LootArchiveApp_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }
  }

  private static final class SingletonCImpl extends LootArchiveApp_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<DataStore<Preferences>> provideDataStoreProvider;

    private Provider<SettingsRepository> settingsRepositoryProvider;

    private Provider<BackupReminderWorker_AssistedFactory> backupReminderWorker_AssistedFactoryProvider;

    private Provider<AppDatabase> provideDatabaseProvider;

    private Provider<ItemRepository> itemRepositoryProvider;

    private Provider<WarrantyCheckWorker_AssistedFactory> warrantyCheckWorker_AssistedFactoryProvider;

    private Provider<CategoryRepository> categoryRepositoryProvider;

    private Provider<BackupRepository> backupRepositoryProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    private ItemDao itemDao() {
      return DatabaseModule_ProvideItemDaoFactory.provideItemDao(provideDatabaseProvider.get());
    }

    private CategoryDao categoryDao() {
      return DatabaseModule_ProvideCategoryDaoFactory.provideCategoryDao(provideDatabaseProvider.get());
    }

    private ItemPhotoDao itemPhotoDao() {
      return DatabaseModule_ProvideItemPhotoDaoFactory.provideItemPhotoDao(provideDatabaseProvider.get());
    }

    private Map<String, javax.inject.Provider<WorkerAssistedFactory<? extends ListenableWorker>>> mapOfStringAndProviderOfWorkerAssistedFactoryOf(
        ) {
      return MapBuilder.<String, javax.inject.Provider<WorkerAssistedFactory<? extends ListenableWorker>>>newMapBuilder(2).put("com.nanji.lootarchive.worker.BackupReminderWorker", ((Provider) backupReminderWorker_AssistedFactoryProvider)).put("com.nanji.lootarchive.worker.WarrantyCheckWorker", ((Provider) warrantyCheckWorker_AssistedFactoryProvider)).build();
    }

    private HiltWorkerFactory hiltWorkerFactory() {
      return WorkerFactoryModule_ProvideFactoryFactory.provideFactory(mapOfStringAndProviderOfWorkerAssistedFactoryOf());
    }

    private BackupRecordDao backupRecordDao() {
      return DatabaseModule_ProvideBackupRecordDaoFactory.provideBackupRecordDao(provideDatabaseProvider.get());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.provideDataStoreProvider = DoubleCheck.provider(new SwitchingProvider<DataStore<Preferences>>(singletonCImpl, 2));
      this.settingsRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<SettingsRepository>(singletonCImpl, 1));
      this.backupReminderWorker_AssistedFactoryProvider = SingleCheck.provider(new SwitchingProvider<BackupReminderWorker_AssistedFactory>(singletonCImpl, 0));
      this.provideDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<AppDatabase>(singletonCImpl, 5));
      this.itemRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<ItemRepository>(singletonCImpl, 4));
      this.warrantyCheckWorker_AssistedFactoryProvider = SingleCheck.provider(new SwitchingProvider<WarrantyCheckWorker_AssistedFactory>(singletonCImpl, 3));
      this.categoryRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<CategoryRepository>(singletonCImpl, 6));
      this.backupRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<BackupRepository>(singletonCImpl, 7));
    }

    @Override
    public void injectLootArchiveApp(LootArchiveApp lootArchiveApp) {
      injectLootArchiveApp2(lootArchiveApp);
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return Collections.<Boolean>emptySet();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private LootArchiveApp injectLootArchiveApp2(LootArchiveApp instance) {
      LootArchiveApp_MembersInjector.injectWorkerFactory(instance, hiltWorkerFactory());
      return instance;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.nanji.lootarchive.worker.BackupReminderWorker_AssistedFactory 
          return (T) new BackupReminderWorker_AssistedFactory() {
            @Override
            public BackupReminderWorker create(Context context, WorkerParameters workerParams) {
              return new BackupReminderWorker(context, workerParams, singletonCImpl.settingsRepositoryProvider.get());
            }
          };

          case 1: // com.nanji.lootarchive.data.repository.SettingsRepository 
          return (T) new SettingsRepository(singletonCImpl.provideDataStoreProvider.get());

          case 2: // androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> 
          return (T) AppModule_ProvideDataStoreFactory.provideDataStore(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 3: // com.nanji.lootarchive.worker.WarrantyCheckWorker_AssistedFactory 
          return (T) new WarrantyCheckWorker_AssistedFactory() {
            @Override
            public WarrantyCheckWorker create(Context context2, WorkerParameters workerParams2) {
              return new WarrantyCheckWorker(context2, workerParams2, singletonCImpl.itemRepositoryProvider.get(), singletonCImpl.settingsRepositoryProvider.get());
            }
          };

          case 4: // com.nanji.lootarchive.data.repository.ItemRepository 
          return (T) new ItemRepository(singletonCImpl.itemDao(), singletonCImpl.categoryDao(), singletonCImpl.itemPhotoDao());

          case 5: // com.nanji.lootarchive.data.local.database.AppDatabase 
          return (T) DatabaseModule_ProvideDatabaseFactory.provideDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 6: // com.nanji.lootarchive.data.repository.CategoryRepository 
          return (T) new CategoryRepository(singletonCImpl.categoryDao(), singletonCImpl.itemDao());

          case 7: // com.nanji.lootarchive.data.repository.BackupRepository 
          return (T) new BackupRepository(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.backupRecordDao());

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
