package com.ding.sync.core;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by xwding on 4/28/2018.
 */

public class SyncUtil {
    public static final String TAG = "Sync";

    private static Map<Class<?>, Map<String, Field>> mFields = new ConcurrentHashMap<>();
    private static Map<Class<?>, Map<Field, Syncable>> mSyncables = new ConcurrentHashMap<>();

    private SyncUtil() {
    }

    static Map<String, Field> getFields(Class<?> clazz) {
        Map<String, Field> fieldMap = mFields.get(clazz);
        if (fieldMap == null) {
            registerClass(clazz);
            fieldMap = mFields.get(clazz);
        }
        return fieldMap;
    }

    static Map<Field, Syncable> getSyncables(Class<?> clazz) {
        Map<Field, Syncable> syncableMap = mSyncables.get(clazz);
        if (syncableMap == null) {
            registerClass(clazz);
            syncableMap = mSyncables.get(clazz);
        }
        return syncableMap;
    }

    static synchronized void registerClass(Class<?> clazz) {
        if (mSyncables.containsKey(clazz)) {
            return;
        }

        try {
            Map<Field, Syncable> syncableFields = new HashMap<>();
            Map<String, Field> syncables = new HashMap<>();
            List<Field> fields = SyncUtil.getAllFields(clazz);
            for (Field field : fields) {
                Syncable syncable = field.getAnnotation(Syncable.class);
                if (syncable != null) {
                    if (!field.isAccessible()) {
                        field.setAccessible(true);
                    }
                    syncableFields.put(field, syncable);
                    syncables.put(syncable.name(), field);
                }
            }
            mSyncables.put(clazz, syncableFields);
            mFields.put(clazz, syncables);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    static void toBundle(Bundle bundle, Class<?> clazz, String key, @Nullable Object value) {
        if (isExtend(clazz, Bundle.class)) {
            bundle.putBundle(key, (Bundle) value);
        } else if (clazz.equals(byte.class) || clazz.equals(Byte.class)) {
            bundle.putByte(key, (Byte) value);
        } else if (clazz.equals(byte[].class)) {
            bundle.putByteArray(key, (byte[]) value);
        } else if (clazz.equals(char.class) || clazz.equals(Character.class)) {
            bundle.putChar(key, (Character) value);
        } else if (isExtend(clazz, CharSequence.class)) {
            bundle.putCharSequence(key, (CharSequence) value);
        } else if (clazz.equals(CharSequence[].class)) {
            bundle.putCharSequenceArray(key, (CharSequence[]) value);
        } else if (clazz.equals(float.class) || clazz.equals(Float.class)) {
            bundle.putFloat(key, (Float) value);
        } else if (clazz.equals(float[].class)) {
            bundle.putByteArray(key, (byte[]) value);
        } else if (clazz.equals(int.class) || clazz.equals(Integer.class)) {
            bundle.putInt(key, (Integer) value);
        } else if (clazz.equals(int[].class)) {
            bundle.putIntArray(key, (int[]) value);
        } else if (isExtend(clazz, Parcelable.class)) {
            bundle.putParcelable(key, (Parcelable) value);
        } else if (clazz.equals(Parcelable[].class)) {
            bundle.putParcelableArray(key, (Parcelable[]) value);
        } else if (isExtend(clazz, Serializable.class)) {
            bundle.putSerializable(key, (Serializable) value);
        } else if (clazz.equals(short.class) || clazz.equals(Short.class)) {
            bundle.putShort(key, (Short) value);
        } else if (clazz.equals(short[].class)) {
            bundle.putShortArray(key, (short[]) value);
        } else if (clazz.equals(String.class)) {
            bundle.putString(key, (String) value);
        } else if (clazz.equals(String[].class)) {
            bundle.putStringArray(key, (String[]) value);
        } else if (clazz.equals(boolean.class) || clazz.equals(Boolean.class)) {
            bundle.putBoolean(key, (Boolean) value);
        } else if (clazz.equals(boolean[].class)) {
            bundle.putBooleanArray(key, (boolean[]) value);
        } else if (clazz.equals(double.class) || clazz.equals(Double.class)) {
            bundle.putDouble(key, (Double) value);
        } else if (clazz.equals(double[].class)) {
            bundle.putDoubleArray(key, (double[]) value);
        } else if (clazz.equals(long.class) || clazz.equals(Long.class)) {
            bundle.putLong(key, (Long) value);
        } else if (clazz.equals(long[].class)) {
            bundle.putLongArray(key, (long[]) value);
        } else if (clazz.equals(ArrayList.class)) {
            Type type = getActualType(clazz);
            if (String.class.equals(type)) {
                bundle.putIntegerArrayList(key, (ArrayList<Integer>) value);
            } else if (isExtend(type, Parcelable.class)) {
                bundle.putParcelableArrayList(key, (ArrayList<? extends Parcelable>) value);
            } else if (isExtend(type, CharSequence.class)) {
                bundle.putCharSequenceArrayList(key, (ArrayList<CharSequence>) value);
            } else {
                throw new UnsupportedOperationException(SyncUtil.TAG + ", Invalid class for bundle, please implement it");
            }
        } else {
            throw new UnsupportedOperationException(SyncUtil.TAG + ", Invalid class for bundle, please implement it");
        }
    }

    static boolean isExtend(Type subClass, Class superClass) {
        if (subClass.equals(subClass)) {
            return true;
        }
        if (subClass instanceof Class) {
            return isExtend((Class<?>) superClass, superClass);
        }
        return false;
    }

    static boolean isExtend(Class<?> subClass, Class superClass) {
        if (subClass == null || superClass == null) {
            return false;
        }
        if (subClass.equals(superClass)) {
            return true;
        }
        Class<?> subSuperClass = subClass.getSuperclass();
        if (subSuperClass != null) {
            if (isExtend(subSuperClass, superClass)) {
                return true;
            }
        }
        Class<?>[] interfaces = subClass.getInterfaces();
        if (interfaces != null) {
            for (Class<?> inter : interfaces) {
                if (isExtend(inter, superClass)) {
                    return true;
                }
            }
        }
        return false;
    }

    static Type getActualType(Class<?> clazz) {
        Type[] types = ((ParameterizedType) clazz.getGenericSuperclass()).getActualTypeArguments();
        if (types != null) {
            return types[0];
        }
        return null;
    }

    static Class<? extends SyncObj> getGenericType(Class<?> listenerClass, Class<?> interfaceClass) {
        Type[] interfaces = listenerClass.getGenericInterfaces();
        if (interfaces != null) {
            for (Type type : interfaces) {
                if (type instanceof ParameterizedType) {
                    ParameterizedType paramType = (ParameterizedType) type;
                    if (paramType.getRawType().equals(interfaceClass)) {
                        return (Class<? extends SyncObj>) paramType.getActualTypeArguments()[0];
                    }
                }
            }
        }

        throw new IllegalArgumentException(SyncUtil.TAG + ", Please set generic type to interface " + interfaceClass.getName());
    }

    static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fieldList = null;
        Class<?> superClazz = clazz.getSuperclass();
        if (superClazz != null && !superClazz.equals(Object.class)) {
            fieldList = getAllFields(superClazz);
        }
        if (fieldList == null) {
            fieldList = new LinkedList<>();
        }
        Field[] fields = clazz.getDeclaredFields();
        if (fields != null && fields.length != 0) {
            for (Field field : fields) {
                fieldList.add(field);
            }
        }
        return fieldList;
    }

}
