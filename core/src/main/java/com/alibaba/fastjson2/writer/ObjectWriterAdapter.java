package com.alibaba.fastjson2.writer;

import com.alibaba.fastjson2.*;
import com.alibaba.fastjson2.codec.FieldInfo;
import com.alibaba.fastjson2.filter.*;
import com.alibaba.fastjson2.util.BeanUtils;
import com.alibaba.fastjson2.util.DateUtils;
import com.alibaba.fastjson2.util.Fnv;
import com.alibaba.fastjson2.util.TypeUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static com.alibaba.fastjson2.JSONB.Constants.BC_TYPED_ANY;
import static com.alibaba.fastjson2.JSONWriter.Feature.*;

public class ObjectWriterAdapter<T>
        implements ObjectWriter<T> {
    boolean hasFilter;
    PropertyPreFilter propertyPreFilter;
    PropertyFilter propertyFilter;
    NameFilter nameFilter;
    ValueFilter valueFilter;

    static final String TYPE = "@type";

    final Class objectClass;
    final List<FieldWriter> fieldWriters;
    protected final FieldWriter[] fieldWriterArray;

    final String typeKey;
    byte[] typeKeyJSONB;
    protected final String typeName;
    protected final long typeNameHash;
    protected long typeNameSymbolCache;
    protected final byte[] typeNameJSONB;

    byte[] nameWithColonUTF8;
    char[] nameWithColonUTF16;

    final long features;

    final long[] hashCodes;
    final short[] mapping;

    final boolean hasValueField;
    final boolean serializable;
    final boolean containsNoneFieldGetter;
    final boolean googleCollection;

    public ObjectWriterAdapter(Class<T> objectClass, List<FieldWriter> fieldWriters) {
        this(objectClass, null, null, 0, fieldWriters);
    }

    public ObjectWriterAdapter(
            Class<T> objectClass,
            String typeKey,
            String typeName,
            long features,
            List<FieldWriter> fieldWriters
    ) {
        if (typeName == null && objectClass != null) {
            if (Enum.class.isAssignableFrom(objectClass) && !objectClass.isEnum()) {
                typeName = objectClass.getSuperclass().getName();
            } else {
                typeName = TypeUtils.getTypeName(objectClass);
            }
        }

        this.objectClass = objectClass;
        this.typeKey = typeKey == null || typeKey.isEmpty() ? TYPE : typeKey;
        this.typeName = typeName;
        this.typeNameHash = typeName != null ? Fnv.hashCode64(typeName) : 0;
        this.typeNameJSONB = JSONB.toBytes(typeName);
        this.features = features;
        this.fieldWriters = fieldWriters;
        this.serializable = objectClass == null || java.io.Serializable.class.isAssignableFrom(objectClass);
        this.googleCollection =
                "com.google.common.collect.AbstractMapBasedMultimap$RandomAccessWrappedList".equals(typeName)
                || "com.google.common.collect.AbstractMapBasedMultimap$WrappedSet".equals(typeName);

        this.fieldWriterArray = new FieldWriter[fieldWriters.size()];
        fieldWriters.toArray(fieldWriterArray);

        this.hasValueField = fieldWriterArray.length == 1 && (fieldWriterArray[0].features & FieldInfo.VALUE_MASK) != 0;

        boolean containsNoneFieldGetter = false;
        long[] hashCodes = new long[fieldWriterArray.length];
        for (int i = 0; i < fieldWriterArray.length; i++) {
            FieldWriter fieldWriter = fieldWriterArray[i];
            long hashCode = Fnv.hashCode64(fieldWriter.fieldName);
            hashCodes[i] = hashCode;

            if (fieldWriter.method != null && (fieldWriter.features & FieldInfo.FIELD_MASK) == 0) {
                containsNoneFieldGetter = true;
            }
        }
        this.containsNoneFieldGetter = containsNoneFieldGetter;

        this.hashCodes = Arrays.copyOf(hashCodes, hashCodes.length);
        Arrays.sort(this.hashCodes);

        mapping = new short[this.hashCodes.length];
        for (int i = 0; i < hashCodes.length; i++) {
            long hashCode = hashCodes[i];
            int index = Arrays.binarySearch(this.hashCodes, hashCode);
            mapping[index] = (short) i;
        }
    }

    @Override
    public long getFeatures() {
        return features;
    }

    @Override
    public FieldWriter getFieldWriter(long hashCode) {
        int m = Arrays.binarySearch(hashCodes, hashCode);
        if (m < 0) {
            return null;
        }

        int index = this.mapping[m];
        return fieldWriterArray[index];
    }

    @Override
    public final boolean hasFilter(JSONWriter jsonWriter) {
        return hasFilter | jsonWriter.hasFilter(containsNoneFieldGetter);
    }

    protected final boolean hasFilter0(JSONWriter jsonWriter) {
        return hasFilter | jsonWriter.hasFilter();
    }

    public void setPropertyFilter(PropertyFilter propertyFilter) {
        this.propertyFilter = propertyFilter;
        if (propertyFilter != null) {
            hasFilter = true;
        }
    }

    public void setValueFilter(ValueFilter valueFilter) {
        this.valueFilter = valueFilter;
        if (valueFilter != null) {
            hasFilter = true;
        }
    }

    public void setNameFilter(NameFilter nameFilter) {
        this.nameFilter = nameFilter;
        if (nameFilter != null) {
            hasFilter = true;
        }
    }

    public void setPropertyPreFilter(PropertyPreFilter propertyPreFilter) {
        this.propertyPreFilter = propertyPreFilter;
        if (propertyPreFilter != null) {
            hasFilter = true;
        }
    }

    @Override
    public void writeArrayMappingJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
        if (jsonWriter.isWriteTypeInfo(object, fieldType, features)) {
            writeClassInfo(jsonWriter);
        }

        int size = fieldWriters.size();
        jsonWriter.startArray(size);
        for (int i = 0; i < size; ++i) {
            FieldWriter fieldWriter = fieldWriters.get(i);
            fieldWriter.writeValue(jsonWriter, object);
        }
    }

    @Override
    public void writeJSONB(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
        long featuresAll = features | this.features | jsonWriter.getFeatures();

        if (!serializable) {
            if ((featuresAll & JSONWriter.Feature.ErrorOnNoneSerializable.mask) != 0) {
                errorOnNoneSerializable();
                return;
            }

            if ((featuresAll & JSONWriter.Feature.IgnoreNoneSerializable.mask) != 0) {
                jsonWriter.writeNull();
                return;
            }
        }

        if ((featuresAll & JSONWriter.Feature.IgnoreNoneSerializable.mask) != 0) {
            writeWithFilter(jsonWriter, object, fieldName, fieldType, features);
            return;
        }

        int size = fieldWriterArray.length;
        if (jsonWriter.isWriteTypeInfo(object, fieldType, features)) {
            writeClassInfo(jsonWriter);
        }
        jsonWriter.startObject();
        for (int i = 0; i < size; ++i) {
            fieldWriters.get(i)
                    .write(jsonWriter, object);
        }
        jsonWriter.endObject();
    }

    protected final void writeClassInfo(JSONWriter jsonWriter) {
        SymbolTable symbolTable = jsonWriter.symbolTable;
        if (symbolTable != null) {
            if (writeClassInfoSymbol(jsonWriter, symbolTable)) {
                return;
            }
        }

        jsonWriter.writeTypeName(typeNameJSONB, typeNameHash);
    }

    private boolean writeClassInfoSymbol(JSONWriter jsonWriter, SymbolTable symbolTable) {
        int symbolTableIdentity = System.identityHashCode(symbolTable);

        int symbol;
        if (typeNameSymbolCache == 0) {
            symbol = symbolTable.getOrdinalByHashCode(typeNameHash);
            if (symbol != -1) {
                typeNameSymbolCache = ((long) symbol << 32) | symbolTableIdentity;
            }
        } else {
            int identity = (int) typeNameSymbolCache;
            if (identity == symbolTableIdentity) {
                symbol = (int) (typeNameSymbolCache >> 32);
            } else {
                symbol = symbolTable.getOrdinalByHashCode(typeNameHash);
                if (symbol != -1) {
                    typeNameSymbolCache = ((long) symbol << 32) | symbolTableIdentity;
                }
            }
        }

        if (symbol != -1) {
            jsonWriter.writeRaw(BC_TYPED_ANY);
            jsonWriter.writeInt32(-symbol);
            return true;
        }
        return false;
    }

    @Override
    public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
        if (hasValueField) {
            FieldWriter fieldWriter = fieldWriterArray[0];
            fieldWriter.writeValue(jsonWriter, object);
            return;
        }

        long featuresAll = features | this.features | jsonWriter.getFeatures();
        boolean beanToArray = (featuresAll & BeanToArray.mask) != 0;

        if (jsonWriter.jsonb) {
            if (beanToArray) {
                writeArrayMappingJSONB(jsonWriter, object, fieldName, fieldType, features);
                return;
            }

            writeJSONB(jsonWriter, object, fieldName, fieldType, features);
            return;
        }

        if (googleCollection) {
            Collection collection = (Collection) object;
            ObjectWriterImplCollection.INSTANCE.write(jsonWriter, collection, fieldName, fieldType, features);
            return;
        }

        if (beanToArray) {
            writeArrayMapping(jsonWriter, object, fieldName, fieldType, features);
            return;
        }

        if (!serializable) {
            if ((featuresAll & JSONWriter.Feature.ErrorOnNoneSerializable.mask) != 0) {
                errorOnNoneSerializable();
                return;
            }

            if ((featuresAll & JSONWriter.Feature.IgnoreNoneSerializable.mask) != 0) {
                jsonWriter.writeNull();
                return;
            }
        }

        if (hasFilter(jsonWriter)) {
            writeWithFilter(jsonWriter, object, fieldName, fieldType, features);
            return;
        }

        jsonWriter.startObject();

        if (((features | this.features) & WriteClassName.mask) != 0 || jsonWriter.isWriteTypeInfo(object, features)) {
            writeTypeInfo(jsonWriter);
        }

        final int size = fieldWriters.size();
        for (int i = 0; i < size; i++) {
            FieldWriter fieldWriter = fieldWriters.get(i);
            fieldWriter.write(jsonWriter, object);
        }

        jsonWriter.endObject();
    }

    public Map<String, Object> toMap(Object object) {
        final int size = fieldWriters.size();
        JSONObject map = new JSONObject(size, 1F);
        for (int i = 0; i < size; i++) {
            FieldWriter fieldWriter = fieldWriters.get(i);
            map.put(
                    fieldWriter.fieldName,
                    fieldWriter.getFieldValue(object)
            );
        }
        return map;
    }

    @Override
    public List<FieldWriter> getFieldWriters() {
        return fieldWriters;
    }

    byte[] jsonbClassInfo;

    @Override
    public boolean writeTypeInfo(JSONWriter jsonWriter) {
        if (jsonWriter.utf8) {
            if (nameWithColonUTF8 == null) {
                int typeKeyLength = typeKey.length();
                int typeNameLength = typeName.length();
                byte[] chars = new byte[typeKeyLength + typeNameLength + 5];
                chars[0] = '"';
                typeKey.getBytes(0, typeKeyLength, chars, 1);
                chars[typeKeyLength + 1] = '"';
                chars[typeKeyLength + 2] = ':';
                chars[typeKeyLength + 3] = '"';
                typeName.getBytes(0, typeNameLength, chars, typeKeyLength + 4);
                chars[typeKeyLength + typeNameLength + 4] = '"';

                nameWithColonUTF8 = chars;
            }
            jsonWriter.writeNameRaw(nameWithColonUTF8);
            return true;
        } else if (jsonWriter.utf16) {
            if (nameWithColonUTF16 == null) {
                int typeKeyLength = typeKey.length();
                int typeNameLength = typeName.length();
                char[] chars = new char[typeKeyLength + typeNameLength + 5];
                chars[0] = '"';
                typeKey.getChars(0, typeKeyLength, chars, 1);
                chars[typeKeyLength + 1] = '"';
                chars[typeKeyLength + 2] = ':';
                chars[typeKeyLength + 3] = '"';
                typeName.getChars(0, typeNameLength, chars, typeKeyLength + 4);
                chars[typeKeyLength + typeNameLength + 4] = '"';

                nameWithColonUTF16 = chars;
            }
            jsonWriter.writeNameRaw(nameWithColonUTF16);
            return true;
        } else if (jsonWriter.jsonb) {
            if (typeKeyJSONB == null) {
                typeKeyJSONB = JSONB.toBytes(typeKey);
            }
            jsonWriter.writeRaw(typeKeyJSONB);
            jsonWriter.writeRaw(typeNameJSONB);
            return true;
        }

        jsonWriter.writeString(typeKey);
        jsonWriter.writeColon();
        jsonWriter.writeString(typeName);
        return true;
    }

    @Override
    public void writeWithFilter(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
        if (object == null) {
            jsonWriter.writeNull();
            return;
        }

        if (jsonWriter.isWriteTypeInfo(object, fieldType, features)) {
            if (jsonWriter.jsonb) {
                writeClassInfo(jsonWriter);
                jsonWriter.startObject();
            } else {
                jsonWriter.startObject();
                writeTypeInfo(jsonWriter);
            }
        } else {
            jsonWriter.startObject();
        }

        JSONWriter.Context context = jsonWriter.context;
        long features2 = context.getFeatures() | features;
        boolean refDetect = (features2 & ReferenceDetection.mask) != 0;
        boolean ignoreNonFieldGetter = (features2 & IgnoreNonFieldGetter.mask) != 0;

        BeforeFilter beforeFilter = context.getBeforeFilter();
        if (beforeFilter != null) {
            beforeFilter.writeBefore(jsonWriter, object);
        }

        PropertyPreFilter propertyPreFilter = context.getPropertyPreFilter();
        if (propertyPreFilter == null) {
            propertyPreFilter = this.propertyPreFilter;
        }

        NameFilter nameFilter = context.getNameFilter();
        if (nameFilter == null) {
            nameFilter = this.nameFilter;
        } else {
            if (this.nameFilter != null) {
                nameFilter = NameFilter.compose(this.nameFilter, nameFilter);
            }
        }

        ContextNameFilter contextNameFilter = context.getContextNameFilter();

        ValueFilter valueFilter = context.getValueFilter();
        if (valueFilter == null) {
            valueFilter = this.valueFilter;
        } else {
            if (this.valueFilter != null) {
                valueFilter = ValueFilter.compose(this.valueFilter, valueFilter);
            }
        }

        ContextValueFilter contextValueFilter = context.getContextValueFilter();

        PropertyFilter propertyFilter = context.getPropertyFilter();
        if (propertyFilter == null) {
            propertyFilter = this.propertyFilter;
        }

        LabelFilter labelFilter = context.getLabelFilter();

        for (int i = 0; i < fieldWriters.size(); i++) {
            FieldWriter fieldWriter = fieldWriters.get(i);
            Field field = fieldWriter.field;

            if (ignoreNonFieldGetter
                    && fieldWriter.method != null
                    && (fieldWriter.features & FieldInfo.FIELD_MASK) == 0) {
                continue;
            }

            // pre property filter
            final String fieldWriterFieldName = fieldWriter.fieldName;
            if (propertyPreFilter != null
                    && !propertyPreFilter.process(jsonWriter, object, fieldWriterFieldName)) {
                continue;
            }

            if (labelFilter != null) {
                String label = fieldWriter.label;
                if (label != null && !label.isEmpty()) {
                    if (!labelFilter.apply(label)) {
                        continue;
                    }
                }
            }

            // fast return
            if (nameFilter == null
                    && propertyFilter == null
                    && contextValueFilter == null
                    && contextNameFilter == null
                    && valueFilter == null
            ) {
                fieldWriter.write(jsonWriter, object);
                continue;
            }

            Object fieldValue;
            try {
                fieldValue = fieldWriter.getFieldValue(object);
            } catch (Throwable e) {
                if ((context.getFeatures() & JSONWriter.Feature.IgnoreErrorGetter.mask) != 0) {
                    continue;
                }
                throw e;
            }

            if (fieldValue == null && !jsonWriter.isWriteNulls()) {
                continue;
            }

            if (!refDetect && ("this$0".equals(fieldWriterFieldName) || "this$1".equals(fieldWriterFieldName) || "this$2".equals(fieldWriterFieldName))) {
                continue;
            }

            BeanContext beanContext = null;

            // name filter
            String filteredName = fieldWriterFieldName;
            if (nameFilter != null) {
                filteredName = nameFilter.process(object, filteredName, fieldValue);
            }

            if (contextNameFilter != null) {
                if (beanContext == null) {
                    if (field == null && fieldWriter.method != null) {
                        field = BeanUtils.getDeclaredField(objectClass, fieldWriter.fieldName);
                    }

                    beanContext = new BeanContext(
                            objectClass,
                            fieldWriter.method,
                            field,
                            fieldWriter.fieldName,
                            fieldWriter.label,
                            fieldWriter.fieldClass,
                            fieldWriter.fieldType,
                            fieldWriter.features,
                            fieldWriter.format
                    );
                    filteredName = contextNameFilter.process(beanContext, object, filteredName, fieldValue);
                }
            }

            // property filter
            if (propertyFilter != null
                    && !propertyFilter.apply(object, fieldWriterFieldName, fieldValue)) {
                continue;
            }

            boolean nameChanged = filteredName != null && filteredName != fieldWriterFieldName;

            Object filteredValue = fieldValue;
            if (valueFilter != null) {
                filteredValue = valueFilter.apply(object, fieldWriterFieldName, fieldValue);
            }
            if (contextValueFilter != null) {
                if (beanContext == null) {
                    if (field == null && fieldWriter.method != null) {
                        field = BeanUtils.getDeclaredField(objectClass, fieldWriter.fieldName);
                    }

                    beanContext = new BeanContext(
                            objectClass,
                            fieldWriter.method,
                            field,
                            fieldWriter.fieldName,
                            fieldWriter.label,
                            fieldWriter.fieldClass,
                            fieldWriter.fieldType,
                            fieldWriter.features,
                            fieldWriter.format
                    );
                }
                filteredValue = contextValueFilter.process(beanContext, object, filteredName, filteredValue);
            }

            if (filteredValue != fieldValue) {
                if (nameChanged) {
                    jsonWriter.writeName(filteredName);
                    jsonWriter.writeColon();
                } else {
                    fieldWriter.writeFieldName(jsonWriter);
                }

                if (filteredValue == null) {
                    jsonWriter.writeNull();
                } else {
                    ObjectWriter fieldValueWriter = fieldWriter.getObjectWriter(jsonWriter, filteredValue.getClass());
                    fieldValueWriter.write(jsonWriter, filteredValue, fieldName, fieldType, features);
                }
            } else {
                if (!nameChanged) {
                    fieldWriter.write(jsonWriter, object);
                } else {
                    jsonWriter.writeName(filteredName);
                    jsonWriter.writeColon();

                    if (fieldValue == null) {
                        ObjectWriter fieldValueWriter = fieldWriter.getObjectWriter(jsonWriter, fieldWriter.fieldClass);
                        fieldValueWriter.write(jsonWriter, null, fieldName, fieldType, features);
                    } else {
                        ObjectWriter fieldValueWriter = fieldWriter.getObjectWriter(jsonWriter, fieldValue.getClass());
                        fieldValueWriter.write(jsonWriter, fieldValue, fieldName, fieldType, features);
                    }
                }
            }
        }

        AfterFilter afterFilter = context.getAfterFilter();
        if (afterFilter != null) {
            afterFilter.writeAfter(jsonWriter, object);
        }

        jsonWriter.endObject();
    }

    public JSONObject toJSONObject(T object) {
        return toJSONObject(object, 0);
    }

    public JSONObject toJSONObject(T object, long features) {
        JSONObject jsonObject = new JSONObject();

        for (int i = 0, size = fieldWriters.size(); i < size; i++) {
            FieldWriter fieldWriter = fieldWriters.get(i);
            Object fieldValue = fieldWriter.getFieldValue(object);
            String format = fieldWriter.format;
            Class fieldClass = fieldWriter.fieldClass;
            if (format != null) {
                if (fieldClass == Date.class) {
                    if ("millis".equals(format)) {
                        fieldValue = ((Date) fieldValue).getTime();
                    } else {
                        fieldValue = DateUtils.format((Date) fieldValue, format);
                    }
                } else if (fieldClass == LocalDate.class) {
                    fieldValue = DateUtils.format((LocalDate) fieldValue, format);
                } else if (fieldClass == LocalDateTime.class) {
                    fieldValue = DateUtils.format((LocalDateTime) fieldValue, format);
                }
            }

            long fieldFeatures = fieldWriter.features;
            if ((fieldFeatures & FieldInfo.UNWRAPPED_MASK) != 0) {
                if (fieldValue instanceof Map) {
                    jsonObject.putAll((Map) fieldValue);
                    continue;
                }

                ObjectWriter fieldObjectWriter = fieldWriter.getInitWriter();
                if (fieldObjectWriter == null) {
                    fieldObjectWriter = JSONFactory.getDefaultObjectWriterProvider().getObjectWriter(fieldClass);
                }
                List<FieldWriter> unwrappedFieldWriters = fieldObjectWriter.getFieldWriters();
                for (int j = 0, unwrappedSize = unwrappedFieldWriters.size(); j < unwrappedSize; j++) {
                    FieldWriter unwrappedFieldWriter = unwrappedFieldWriters.get(j);
                    Object unwrappedFieldValue = unwrappedFieldWriter.getFieldValue(fieldValue);
                    jsonObject.put(unwrappedFieldWriter.fieldName, unwrappedFieldValue);
                }
                continue;
            }

            if (fieldValue != null) {
                String fieldValueClassName = fieldValue.getClass().getName();
                if (Collection.class.isAssignableFrom(fieldClass)
                        && fieldValue.getClass() != JSONObject.class
                        && !fieldValueClassName.equals("com.alibaba.fastjson.JSONObject")
                ) {
                    Collection collection = (Collection) fieldValue;
                    JSONArray array = new JSONArray(collection.size());
                    for (Object item : collection) {
                        Object itemJSON = item == object ? jsonObject : JSON.toJSON(item);
                        array.add(itemJSON);
                    }
                    fieldValue = array;
                }
            }

            if (fieldValue == null && ((this.features | features) & WriteNulls.mask) == 0) {
                continue;
            }

            if (fieldValue == object) {
                fieldValue = jsonObject;
            }
            if (fieldValue instanceof Enum) {
                if ((features & WriteEnumsUsingName.mask) != 0) {
                    fieldValue = ((Enum) fieldValue).name();
                }
            }
            if (fieldWriter instanceof FieldWriterObject && !(fieldValue instanceof Map)) {
                ObjectWriter valueWriter = fieldWriter.getInitWriter();
                if (valueWriter == null) {
                    valueWriter = JSONFactory.getObjectWriter(fieldWriter.fieldType, this.features | features);
                }
                if (valueWriter instanceof ObjectWriterAdapter) {
                    ObjectWriterAdapter objectWriterAdapter = (ObjectWriterAdapter) valueWriter;
                    if (!objectWriterAdapter.getFieldWriters().isEmpty()) {
                        fieldValue = objectWriterAdapter.toJSONObject(fieldValue);
                    } else {
                        fieldValue = JSON.toJSON(fieldValue);
                    }
                }
            }
            jsonObject.put(fieldWriter.fieldName, fieldValue);
        }

        return jsonObject;
    }

    @Override
    public String toString() {
        return objectClass.getName();
    }

    protected void errorOnNoneSerializable() {
        throw new JSONException("not support none serializable class " + objectClass.getName());
    }
}
