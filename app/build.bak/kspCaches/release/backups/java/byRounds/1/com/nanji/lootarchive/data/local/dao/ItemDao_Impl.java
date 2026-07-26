package com.nanji.lootarchive.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.nanji.lootarchive.data.local.entity.ItemEntity;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ItemDao_Impl implements ItemDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ItemEntity> __insertionAdapterOfItemEntity;

  private final EntityDeletionOrUpdateAdapter<ItemEntity> __updateAdapterOfItemEntity;

  private final SharedSQLiteStatement __preparedStmtOfSoftDeleteItem;

  private final SharedSQLiteStatement __preparedStmtOfRestoreItem;

  private final SharedSQLiteStatement __preparedStmtOfHardDeleteItem;

  private final SharedSQLiteStatement __preparedStmtOfEmptyTrash;

  public ItemDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfItemEntity = new EntityInsertionAdapter<ItemEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `items` (`id`,`name`,`categoryId`,`purchasePrice`,`currency`,`storageLocation`,`purchaseDate`,`warrantyExpiryDate`,`warrantyPeriodDays`,`description`,`isDeleted`,`deletedAt`,`createdAt`,`updatedAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ItemEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindLong(3, entity.getCategoryId());
        statement.bindDouble(4, entity.getPurchasePrice());
        statement.bindString(5, entity.getCurrency());
        statement.bindString(6, entity.getStorageLocation());
        if (entity.getPurchaseDate() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getPurchaseDate());
        }
        if (entity.getWarrantyExpiryDate() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getWarrantyExpiryDate());
        }
        if (entity.getWarrantyPeriodDays() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getWarrantyPeriodDays());
        }
        statement.bindString(10, entity.getDescription());
        final int _tmp = entity.isDeleted() ? 1 : 0;
        statement.bindLong(11, _tmp);
        if (entity.getDeletedAt() == null) {
          statement.bindNull(12);
        } else {
          statement.bindLong(12, entity.getDeletedAt());
        }
        statement.bindLong(13, entity.getCreatedAt());
        statement.bindLong(14, entity.getUpdatedAt());
      }
    };
    this.__updateAdapterOfItemEntity = new EntityDeletionOrUpdateAdapter<ItemEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `items` SET `id` = ?,`name` = ?,`categoryId` = ?,`purchasePrice` = ?,`currency` = ?,`storageLocation` = ?,`purchaseDate` = ?,`warrantyExpiryDate` = ?,`warrantyPeriodDays` = ?,`description` = ?,`isDeleted` = ?,`deletedAt` = ?,`createdAt` = ?,`updatedAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ItemEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindLong(3, entity.getCategoryId());
        statement.bindDouble(4, entity.getPurchasePrice());
        statement.bindString(5, entity.getCurrency());
        statement.bindString(6, entity.getStorageLocation());
        if (entity.getPurchaseDate() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getPurchaseDate());
        }
        if (entity.getWarrantyExpiryDate() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getWarrantyExpiryDate());
        }
        if (entity.getWarrantyPeriodDays() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getWarrantyPeriodDays());
        }
        statement.bindString(10, entity.getDescription());
        final int _tmp = entity.isDeleted() ? 1 : 0;
        statement.bindLong(11, _tmp);
        if (entity.getDeletedAt() == null) {
          statement.bindNull(12);
        } else {
          statement.bindLong(12, entity.getDeletedAt());
        }
        statement.bindLong(13, entity.getCreatedAt());
        statement.bindLong(14, entity.getUpdatedAt());
        statement.bindLong(15, entity.getId());
      }
    };
    this.__preparedStmtOfSoftDeleteItem = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE items SET isDeleted = 1, deletedAt = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfRestoreItem = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE items SET isDeleted = 0, deletedAt = NULL WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfHardDeleteItem = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM items WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfEmptyTrash = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM items WHERE isDeleted = 1";
        return _query;
      }
    };
  }

  @Override
  public Object insertItem(final ItemEntity item, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfItemEntity.insertAndReturnId(item);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateItem(final ItemEntity item, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfItemEntity.handle(item);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object softDeleteItem(final long itemId, final long deletedAt,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSoftDeleteItem.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, deletedAt);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, itemId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfSoftDeleteItem.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object restoreItem(final long itemId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfRestoreItem.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, itemId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfRestoreItem.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object hardDeleteItem(final long itemId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfHardDeleteItem.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, itemId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfHardDeleteItem.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object emptyTrash(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfEmptyTrash.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfEmptyTrash.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ItemEntity>> getAllItems() {
    final String _sql = "SELECT * FROM items WHERE isDeleted = 0 ORDER BY updatedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"items"}, new Callable<List<ItemEntity>>() {
      @Override
      @NonNull
      public List<ItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfPurchasePrice = CursorUtil.getColumnIndexOrThrow(_cursor, "purchasePrice");
          final int _cursorIndexOfCurrency = CursorUtil.getColumnIndexOrThrow(_cursor, "currency");
          final int _cursorIndexOfStorageLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "storageLocation");
          final int _cursorIndexOfPurchaseDate = CursorUtil.getColumnIndexOrThrow(_cursor, "purchaseDate");
          final int _cursorIndexOfWarrantyExpiryDate = CursorUtil.getColumnIndexOrThrow(_cursor, "warrantyExpiryDate");
          final int _cursorIndexOfWarrantyPeriodDays = CursorUtil.getColumnIndexOrThrow(_cursor, "warrantyPeriodDays");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<ItemEntity> _result = new ArrayList<ItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ItemEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final long _tmpCategoryId;
            _tmpCategoryId = _cursor.getLong(_cursorIndexOfCategoryId);
            final double _tmpPurchasePrice;
            _tmpPurchasePrice = _cursor.getDouble(_cursorIndexOfPurchasePrice);
            final String _tmpCurrency;
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency);
            final String _tmpStorageLocation;
            _tmpStorageLocation = _cursor.getString(_cursorIndexOfStorageLocation);
            final Long _tmpPurchaseDate;
            if (_cursor.isNull(_cursorIndexOfPurchaseDate)) {
              _tmpPurchaseDate = null;
            } else {
              _tmpPurchaseDate = _cursor.getLong(_cursorIndexOfPurchaseDate);
            }
            final Long _tmpWarrantyExpiryDate;
            if (_cursor.isNull(_cursorIndexOfWarrantyExpiryDate)) {
              _tmpWarrantyExpiryDate = null;
            } else {
              _tmpWarrantyExpiryDate = _cursor.getLong(_cursorIndexOfWarrantyExpiryDate);
            }
            final Integer _tmpWarrantyPeriodDays;
            if (_cursor.isNull(_cursorIndexOfWarrantyPeriodDays)) {
              _tmpWarrantyPeriodDays = null;
            } else {
              _tmpWarrantyPeriodDays = _cursor.getInt(_cursorIndexOfWarrantyPeriodDays);
            }
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final boolean _tmpIsDeleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp != 0;
            final Long _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getLong(_cursorIndexOfDeletedAt);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new ItemEntity(_tmpId,_tmpName,_tmpCategoryId,_tmpPurchasePrice,_tmpCurrency,_tmpStorageLocation,_tmpPurchaseDate,_tmpWarrantyExpiryDate,_tmpWarrantyPeriodDays,_tmpDescription,_tmpIsDeleted,_tmpDeletedAt,_tmpCreatedAt,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getItemById(final long id, final Continuation<? super ItemEntity> $completion) {
    final String _sql = "SELECT * FROM items WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ItemEntity>() {
      @Override
      @Nullable
      public ItemEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfPurchasePrice = CursorUtil.getColumnIndexOrThrow(_cursor, "purchasePrice");
          final int _cursorIndexOfCurrency = CursorUtil.getColumnIndexOrThrow(_cursor, "currency");
          final int _cursorIndexOfStorageLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "storageLocation");
          final int _cursorIndexOfPurchaseDate = CursorUtil.getColumnIndexOrThrow(_cursor, "purchaseDate");
          final int _cursorIndexOfWarrantyExpiryDate = CursorUtil.getColumnIndexOrThrow(_cursor, "warrantyExpiryDate");
          final int _cursorIndexOfWarrantyPeriodDays = CursorUtil.getColumnIndexOrThrow(_cursor, "warrantyPeriodDays");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final ItemEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final long _tmpCategoryId;
            _tmpCategoryId = _cursor.getLong(_cursorIndexOfCategoryId);
            final double _tmpPurchasePrice;
            _tmpPurchasePrice = _cursor.getDouble(_cursorIndexOfPurchasePrice);
            final String _tmpCurrency;
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency);
            final String _tmpStorageLocation;
            _tmpStorageLocation = _cursor.getString(_cursorIndexOfStorageLocation);
            final Long _tmpPurchaseDate;
            if (_cursor.isNull(_cursorIndexOfPurchaseDate)) {
              _tmpPurchaseDate = null;
            } else {
              _tmpPurchaseDate = _cursor.getLong(_cursorIndexOfPurchaseDate);
            }
            final Long _tmpWarrantyExpiryDate;
            if (_cursor.isNull(_cursorIndexOfWarrantyExpiryDate)) {
              _tmpWarrantyExpiryDate = null;
            } else {
              _tmpWarrantyExpiryDate = _cursor.getLong(_cursorIndexOfWarrantyExpiryDate);
            }
            final Integer _tmpWarrantyPeriodDays;
            if (_cursor.isNull(_cursorIndexOfWarrantyPeriodDays)) {
              _tmpWarrantyPeriodDays = null;
            } else {
              _tmpWarrantyPeriodDays = _cursor.getInt(_cursorIndexOfWarrantyPeriodDays);
            }
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final boolean _tmpIsDeleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp != 0;
            final Long _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getLong(_cursorIndexOfDeletedAt);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new ItemEntity(_tmpId,_tmpName,_tmpCategoryId,_tmpPurchasePrice,_tmpCurrency,_tmpStorageLocation,_tmpPurchaseDate,_tmpWarrantyExpiryDate,_tmpWarrantyPeriodDays,_tmpDescription,_tmpIsDeleted,_tmpDeletedAt,_tmpCreatedAt,_tmpUpdatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<ItemEntity> getItemByIdFlow(final long id) {
    final String _sql = "SELECT * FROM items WHERE isDeleted = 0 AND id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"items"}, new Callable<ItemEntity>() {
      @Override
      @Nullable
      public ItemEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfPurchasePrice = CursorUtil.getColumnIndexOrThrow(_cursor, "purchasePrice");
          final int _cursorIndexOfCurrency = CursorUtil.getColumnIndexOrThrow(_cursor, "currency");
          final int _cursorIndexOfStorageLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "storageLocation");
          final int _cursorIndexOfPurchaseDate = CursorUtil.getColumnIndexOrThrow(_cursor, "purchaseDate");
          final int _cursorIndexOfWarrantyExpiryDate = CursorUtil.getColumnIndexOrThrow(_cursor, "warrantyExpiryDate");
          final int _cursorIndexOfWarrantyPeriodDays = CursorUtil.getColumnIndexOrThrow(_cursor, "warrantyPeriodDays");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final ItemEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final long _tmpCategoryId;
            _tmpCategoryId = _cursor.getLong(_cursorIndexOfCategoryId);
            final double _tmpPurchasePrice;
            _tmpPurchasePrice = _cursor.getDouble(_cursorIndexOfPurchasePrice);
            final String _tmpCurrency;
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency);
            final String _tmpStorageLocation;
            _tmpStorageLocation = _cursor.getString(_cursorIndexOfStorageLocation);
            final Long _tmpPurchaseDate;
            if (_cursor.isNull(_cursorIndexOfPurchaseDate)) {
              _tmpPurchaseDate = null;
            } else {
              _tmpPurchaseDate = _cursor.getLong(_cursorIndexOfPurchaseDate);
            }
            final Long _tmpWarrantyExpiryDate;
            if (_cursor.isNull(_cursorIndexOfWarrantyExpiryDate)) {
              _tmpWarrantyExpiryDate = null;
            } else {
              _tmpWarrantyExpiryDate = _cursor.getLong(_cursorIndexOfWarrantyExpiryDate);
            }
            final Integer _tmpWarrantyPeriodDays;
            if (_cursor.isNull(_cursorIndexOfWarrantyPeriodDays)) {
              _tmpWarrantyPeriodDays = null;
            } else {
              _tmpWarrantyPeriodDays = _cursor.getInt(_cursorIndexOfWarrantyPeriodDays);
            }
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final boolean _tmpIsDeleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp != 0;
            final Long _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getLong(_cursorIndexOfDeletedAt);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new ItemEntity(_tmpId,_tmpName,_tmpCategoryId,_tmpPurchasePrice,_tmpCurrency,_tmpStorageLocation,_tmpPurchaseDate,_tmpWarrantyExpiryDate,_tmpWarrantyPeriodDays,_tmpDescription,_tmpIsDeleted,_tmpDeletedAt,_tmpCreatedAt,_tmpUpdatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<ItemEntity>> getItemsByCategory(final long categoryId) {
    final String _sql = "SELECT * FROM items WHERE isDeleted = 0 AND categoryId = ? ORDER BY updatedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, categoryId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"items"}, new Callable<List<ItemEntity>>() {
      @Override
      @NonNull
      public List<ItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfPurchasePrice = CursorUtil.getColumnIndexOrThrow(_cursor, "purchasePrice");
          final int _cursorIndexOfCurrency = CursorUtil.getColumnIndexOrThrow(_cursor, "currency");
          final int _cursorIndexOfStorageLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "storageLocation");
          final int _cursorIndexOfPurchaseDate = CursorUtil.getColumnIndexOrThrow(_cursor, "purchaseDate");
          final int _cursorIndexOfWarrantyExpiryDate = CursorUtil.getColumnIndexOrThrow(_cursor, "warrantyExpiryDate");
          final int _cursorIndexOfWarrantyPeriodDays = CursorUtil.getColumnIndexOrThrow(_cursor, "warrantyPeriodDays");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<ItemEntity> _result = new ArrayList<ItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ItemEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final long _tmpCategoryId;
            _tmpCategoryId = _cursor.getLong(_cursorIndexOfCategoryId);
            final double _tmpPurchasePrice;
            _tmpPurchasePrice = _cursor.getDouble(_cursorIndexOfPurchasePrice);
            final String _tmpCurrency;
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency);
            final String _tmpStorageLocation;
            _tmpStorageLocation = _cursor.getString(_cursorIndexOfStorageLocation);
            final Long _tmpPurchaseDate;
            if (_cursor.isNull(_cursorIndexOfPurchaseDate)) {
              _tmpPurchaseDate = null;
            } else {
              _tmpPurchaseDate = _cursor.getLong(_cursorIndexOfPurchaseDate);
            }
            final Long _tmpWarrantyExpiryDate;
            if (_cursor.isNull(_cursorIndexOfWarrantyExpiryDate)) {
              _tmpWarrantyExpiryDate = null;
            } else {
              _tmpWarrantyExpiryDate = _cursor.getLong(_cursorIndexOfWarrantyExpiryDate);
            }
            final Integer _tmpWarrantyPeriodDays;
            if (_cursor.isNull(_cursorIndexOfWarrantyPeriodDays)) {
              _tmpWarrantyPeriodDays = null;
            } else {
              _tmpWarrantyPeriodDays = _cursor.getInt(_cursorIndexOfWarrantyPeriodDays);
            }
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final boolean _tmpIsDeleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp != 0;
            final Long _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getLong(_cursorIndexOfDeletedAt);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new ItemEntity(_tmpId,_tmpName,_tmpCategoryId,_tmpPurchasePrice,_tmpCurrency,_tmpStorageLocation,_tmpPurchaseDate,_tmpWarrantyExpiryDate,_tmpWarrantyPeriodDays,_tmpDescription,_tmpIsDeleted,_tmpDeletedAt,_tmpCreatedAt,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<ItemEntity>> searchItems(final String keyword) {
    final String _sql = "SELECT * FROM items WHERE isDeleted = 0 AND (name LIKE '%' || ? || '%' OR storageLocation LIKE '%' || ? || '%' OR description LIKE '%' || ? || '%') ORDER BY updatedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindString(_argIndex, keyword);
    _argIndex = 2;
    _statement.bindString(_argIndex, keyword);
    _argIndex = 3;
    _statement.bindString(_argIndex, keyword);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"items"}, new Callable<List<ItemEntity>>() {
      @Override
      @NonNull
      public List<ItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfPurchasePrice = CursorUtil.getColumnIndexOrThrow(_cursor, "purchasePrice");
          final int _cursorIndexOfCurrency = CursorUtil.getColumnIndexOrThrow(_cursor, "currency");
          final int _cursorIndexOfStorageLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "storageLocation");
          final int _cursorIndexOfPurchaseDate = CursorUtil.getColumnIndexOrThrow(_cursor, "purchaseDate");
          final int _cursorIndexOfWarrantyExpiryDate = CursorUtil.getColumnIndexOrThrow(_cursor, "warrantyExpiryDate");
          final int _cursorIndexOfWarrantyPeriodDays = CursorUtil.getColumnIndexOrThrow(_cursor, "warrantyPeriodDays");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<ItemEntity> _result = new ArrayList<ItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ItemEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final long _tmpCategoryId;
            _tmpCategoryId = _cursor.getLong(_cursorIndexOfCategoryId);
            final double _tmpPurchasePrice;
            _tmpPurchasePrice = _cursor.getDouble(_cursorIndexOfPurchasePrice);
            final String _tmpCurrency;
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency);
            final String _tmpStorageLocation;
            _tmpStorageLocation = _cursor.getString(_cursorIndexOfStorageLocation);
            final Long _tmpPurchaseDate;
            if (_cursor.isNull(_cursorIndexOfPurchaseDate)) {
              _tmpPurchaseDate = null;
            } else {
              _tmpPurchaseDate = _cursor.getLong(_cursorIndexOfPurchaseDate);
            }
            final Long _tmpWarrantyExpiryDate;
            if (_cursor.isNull(_cursorIndexOfWarrantyExpiryDate)) {
              _tmpWarrantyExpiryDate = null;
            } else {
              _tmpWarrantyExpiryDate = _cursor.getLong(_cursorIndexOfWarrantyExpiryDate);
            }
            final Integer _tmpWarrantyPeriodDays;
            if (_cursor.isNull(_cursorIndexOfWarrantyPeriodDays)) {
              _tmpWarrantyPeriodDays = null;
            } else {
              _tmpWarrantyPeriodDays = _cursor.getInt(_cursorIndexOfWarrantyPeriodDays);
            }
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final boolean _tmpIsDeleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp != 0;
            final Long _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getLong(_cursorIndexOfDeletedAt);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new ItemEntity(_tmpId,_tmpName,_tmpCategoryId,_tmpPurchasePrice,_tmpCurrency,_tmpStorageLocation,_tmpPurchaseDate,_tmpWarrantyExpiryDate,_tmpWarrantyPeriodDays,_tmpDescription,_tmpIsDeleted,_tmpDeletedAt,_tmpCreatedAt,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<ItemEntity>> filterItems(final Long categoryId, final Long startDate,
      final Long endDate) {
    final String _sql = "\n"
            + "        SELECT * FROM items WHERE isDeleted = 0\n"
            + "        AND (? IS NULL OR categoryId = ?)\n"
            + "        AND (? IS NULL OR purchaseDate >= ?)\n"
            + "        AND (? IS NULL OR purchaseDate <= ?)\n"
            + "        ORDER BY updatedAt DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 6);
    int _argIndex = 1;
    if (categoryId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindLong(_argIndex, categoryId);
    }
    _argIndex = 2;
    if (categoryId == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindLong(_argIndex, categoryId);
    }
    _argIndex = 3;
    if (startDate == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindLong(_argIndex, startDate);
    }
    _argIndex = 4;
    if (startDate == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindLong(_argIndex, startDate);
    }
    _argIndex = 5;
    if (endDate == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindLong(_argIndex, endDate);
    }
    _argIndex = 6;
    if (endDate == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindLong(_argIndex, endDate);
    }
    return CoroutinesRoom.createFlow(__db, false, new String[] {"items"}, new Callable<List<ItemEntity>>() {
      @Override
      @NonNull
      public List<ItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfPurchasePrice = CursorUtil.getColumnIndexOrThrow(_cursor, "purchasePrice");
          final int _cursorIndexOfCurrency = CursorUtil.getColumnIndexOrThrow(_cursor, "currency");
          final int _cursorIndexOfStorageLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "storageLocation");
          final int _cursorIndexOfPurchaseDate = CursorUtil.getColumnIndexOrThrow(_cursor, "purchaseDate");
          final int _cursorIndexOfWarrantyExpiryDate = CursorUtil.getColumnIndexOrThrow(_cursor, "warrantyExpiryDate");
          final int _cursorIndexOfWarrantyPeriodDays = CursorUtil.getColumnIndexOrThrow(_cursor, "warrantyPeriodDays");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<ItemEntity> _result = new ArrayList<ItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ItemEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final long _tmpCategoryId;
            _tmpCategoryId = _cursor.getLong(_cursorIndexOfCategoryId);
            final double _tmpPurchasePrice;
            _tmpPurchasePrice = _cursor.getDouble(_cursorIndexOfPurchasePrice);
            final String _tmpCurrency;
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency);
            final String _tmpStorageLocation;
            _tmpStorageLocation = _cursor.getString(_cursorIndexOfStorageLocation);
            final Long _tmpPurchaseDate;
            if (_cursor.isNull(_cursorIndexOfPurchaseDate)) {
              _tmpPurchaseDate = null;
            } else {
              _tmpPurchaseDate = _cursor.getLong(_cursorIndexOfPurchaseDate);
            }
            final Long _tmpWarrantyExpiryDate;
            if (_cursor.isNull(_cursorIndexOfWarrantyExpiryDate)) {
              _tmpWarrantyExpiryDate = null;
            } else {
              _tmpWarrantyExpiryDate = _cursor.getLong(_cursorIndexOfWarrantyExpiryDate);
            }
            final Integer _tmpWarrantyPeriodDays;
            if (_cursor.isNull(_cursorIndexOfWarrantyPeriodDays)) {
              _tmpWarrantyPeriodDays = null;
            } else {
              _tmpWarrantyPeriodDays = _cursor.getInt(_cursorIndexOfWarrantyPeriodDays);
            }
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final boolean _tmpIsDeleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp != 0;
            final Long _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getLong(_cursorIndexOfDeletedAt);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new ItemEntity(_tmpId,_tmpName,_tmpCategoryId,_tmpPurchasePrice,_tmpCurrency,_tmpStorageLocation,_tmpPurchaseDate,_tmpWarrantyExpiryDate,_tmpWarrantyPeriodDays,_tmpDescription,_tmpIsDeleted,_tmpDeletedAt,_tmpCreatedAt,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<ItemEntity>> getWarrantyExpiringItems(final long threshold) {
    final String _sql = "SELECT * FROM items WHERE isDeleted = 0 AND warrantyExpiryDate IS NOT NULL AND warrantyExpiryDate <= ? ORDER BY warrantyExpiryDate ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, threshold);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"items"}, new Callable<List<ItemEntity>>() {
      @Override
      @NonNull
      public List<ItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfPurchasePrice = CursorUtil.getColumnIndexOrThrow(_cursor, "purchasePrice");
          final int _cursorIndexOfCurrency = CursorUtil.getColumnIndexOrThrow(_cursor, "currency");
          final int _cursorIndexOfStorageLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "storageLocation");
          final int _cursorIndexOfPurchaseDate = CursorUtil.getColumnIndexOrThrow(_cursor, "purchaseDate");
          final int _cursorIndexOfWarrantyExpiryDate = CursorUtil.getColumnIndexOrThrow(_cursor, "warrantyExpiryDate");
          final int _cursorIndexOfWarrantyPeriodDays = CursorUtil.getColumnIndexOrThrow(_cursor, "warrantyPeriodDays");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<ItemEntity> _result = new ArrayList<ItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ItemEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final long _tmpCategoryId;
            _tmpCategoryId = _cursor.getLong(_cursorIndexOfCategoryId);
            final double _tmpPurchasePrice;
            _tmpPurchasePrice = _cursor.getDouble(_cursorIndexOfPurchasePrice);
            final String _tmpCurrency;
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency);
            final String _tmpStorageLocation;
            _tmpStorageLocation = _cursor.getString(_cursorIndexOfStorageLocation);
            final Long _tmpPurchaseDate;
            if (_cursor.isNull(_cursorIndexOfPurchaseDate)) {
              _tmpPurchaseDate = null;
            } else {
              _tmpPurchaseDate = _cursor.getLong(_cursorIndexOfPurchaseDate);
            }
            final Long _tmpWarrantyExpiryDate;
            if (_cursor.isNull(_cursorIndexOfWarrantyExpiryDate)) {
              _tmpWarrantyExpiryDate = null;
            } else {
              _tmpWarrantyExpiryDate = _cursor.getLong(_cursorIndexOfWarrantyExpiryDate);
            }
            final Integer _tmpWarrantyPeriodDays;
            if (_cursor.isNull(_cursorIndexOfWarrantyPeriodDays)) {
              _tmpWarrantyPeriodDays = null;
            } else {
              _tmpWarrantyPeriodDays = _cursor.getInt(_cursorIndexOfWarrantyPeriodDays);
            }
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final boolean _tmpIsDeleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp != 0;
            final Long _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getLong(_cursorIndexOfDeletedAt);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new ItemEntity(_tmpId,_tmpName,_tmpCategoryId,_tmpPurchasePrice,_tmpCurrency,_tmpStorageLocation,_tmpPurchaseDate,_tmpWarrantyExpiryDate,_tmpWarrantyPeriodDays,_tmpDescription,_tmpIsDeleted,_tmpDeletedAt,_tmpCreatedAt,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<Integer> getWarrantyExpiringCount(final long threshold) {
    final String _sql = "SELECT COUNT(*) FROM items WHERE isDeleted = 0 AND warrantyExpiryDate IS NOT NULL AND warrantyExpiryDate <= ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, threshold);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"items"}, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<Integer> getTotalCount() {
    final String _sql = "SELECT COUNT(*) FROM items WHERE isDeleted = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"items"}, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<Double> getTotalValue() {
    final String _sql = "SELECT COALESCE(SUM(purchasePrice), 0) FROM items WHERE isDeleted = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"items"}, new Callable<Double>() {
      @Override
      @NonNull
      public Double call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Double _result;
          if (_cursor.moveToFirst()) {
            final double _tmp;
            _tmp = _cursor.getDouble(0);
            _result = _tmp;
          } else {
            _result = 0.0;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getCategoryTotalValue(final long categoryId,
      final Continuation<? super Double> $completion) {
    final String _sql = "SELECT COALESCE(SUM(purchasePrice), 0) FROM items WHERE isDeleted = 0 AND categoryId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, categoryId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Double>() {
      @Override
      @NonNull
      public Double call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Double _result;
          if (_cursor.moveToFirst()) {
            final double _tmp;
            _tmp = _cursor.getDouble(0);
            _result = _tmp;
          } else {
            _result = 0.0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<Integer> getCategoryItemCount(final long categoryId) {
    final String _sql = "SELECT COUNT(*) FROM items WHERE isDeleted = 0 AND categoryId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, categoryId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"items"}, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<ItemEntity>> getDeletedItems() {
    final String _sql = "SELECT * FROM items WHERE isDeleted = 1 ORDER BY deletedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"items"}, new Callable<List<ItemEntity>>() {
      @Override
      @NonNull
      public List<ItemEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfPurchasePrice = CursorUtil.getColumnIndexOrThrow(_cursor, "purchasePrice");
          final int _cursorIndexOfCurrency = CursorUtil.getColumnIndexOrThrow(_cursor, "currency");
          final int _cursorIndexOfStorageLocation = CursorUtil.getColumnIndexOrThrow(_cursor, "storageLocation");
          final int _cursorIndexOfPurchaseDate = CursorUtil.getColumnIndexOrThrow(_cursor, "purchaseDate");
          final int _cursorIndexOfWarrantyExpiryDate = CursorUtil.getColumnIndexOrThrow(_cursor, "warrantyExpiryDate");
          final int _cursorIndexOfWarrantyPeriodDays = CursorUtil.getColumnIndexOrThrow(_cursor, "warrantyPeriodDays");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDeletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "deletedAt");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<ItemEntity> _result = new ArrayList<ItemEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ItemEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final long _tmpCategoryId;
            _tmpCategoryId = _cursor.getLong(_cursorIndexOfCategoryId);
            final double _tmpPurchasePrice;
            _tmpPurchasePrice = _cursor.getDouble(_cursorIndexOfPurchasePrice);
            final String _tmpCurrency;
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency);
            final String _tmpStorageLocation;
            _tmpStorageLocation = _cursor.getString(_cursorIndexOfStorageLocation);
            final Long _tmpPurchaseDate;
            if (_cursor.isNull(_cursorIndexOfPurchaseDate)) {
              _tmpPurchaseDate = null;
            } else {
              _tmpPurchaseDate = _cursor.getLong(_cursorIndexOfPurchaseDate);
            }
            final Long _tmpWarrantyExpiryDate;
            if (_cursor.isNull(_cursorIndexOfWarrantyExpiryDate)) {
              _tmpWarrantyExpiryDate = null;
            } else {
              _tmpWarrantyExpiryDate = _cursor.getLong(_cursorIndexOfWarrantyExpiryDate);
            }
            final Integer _tmpWarrantyPeriodDays;
            if (_cursor.isNull(_cursorIndexOfWarrantyPeriodDays)) {
              _tmpWarrantyPeriodDays = null;
            } else {
              _tmpWarrantyPeriodDays = _cursor.getInt(_cursorIndexOfWarrantyPeriodDays);
            }
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final boolean _tmpIsDeleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp != 0;
            final Long _tmpDeletedAt;
            if (_cursor.isNull(_cursorIndexOfDeletedAt)) {
              _tmpDeletedAt = null;
            } else {
              _tmpDeletedAt = _cursor.getLong(_cursorIndexOfDeletedAt);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new ItemEntity(_tmpId,_tmpName,_tmpCategoryId,_tmpPurchasePrice,_tmpCurrency,_tmpStorageLocation,_tmpPurchaseDate,_tmpWarrantyExpiryDate,_tmpWarrantyPeriodDays,_tmpDescription,_tmpIsDeleted,_tmpDeletedAt,_tmpCreatedAt,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
