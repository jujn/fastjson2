package com.alibaba.fastjson2;

import com.alibaba.fastjson2.annotation.JSONField;
import com.alibaba.fastjson2.util.Fnv;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.*;
import java.util.Arrays;
import java.util.UUID;

import static com.alibaba.fastjson2.JSONWriter.Feature.BrowserSecure;
import static org.junit.jupiter.api.Assertions.*;

public class JSONWriterJSONBTest {
    @Test
    public void test_startObject() {
        JSONWriter jsonWriter = JSONWriter.ofJSONB();
        for (int i = 0; i < 8096; i++) {
            jsonWriter.startObject();
            jsonWriter.endObject();
        }
    }

    @Test
    public void test_startArray() {
        JSONWriter jsonWriter = JSONWriter.ofJSONB();
        for (int i = 0; i < 8096; i++) {
            jsonWriter.startArray(1);
        }
    }

    @Test
    public void test_writeRaw() {
        JSONWriter jsonWriter = JSONWriter.ofJSONB();
        for (int i = 0; i < 8096; i++) {
            jsonWriter.writeRaw(JSONB.Constants.BC_NULL);
        }
    }

    @Test
    public void test_writeRaw_1() {
        JSONWriter jsonWriter = JSONWriter.ofJSONB();
        for (int i = 0; i < 8096; i++) {
            jsonWriter.writeRaw(new byte[]{JSONB.Constants.BC_NULL});
        }
    }

    @Test
    public void test_writeMillis() {
        JSONWriter jsonWriter = JSONWriter.ofJSONB();
        for (int i = 0; i < 8096; i++) {
            jsonWriter.writeMillis(1);
        }
    }

    @Test
    public void notSupported() {
        JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
        assertThrows(JSONException.class, () -> jsonWriter.startArray());
        assertThrows(JSONException.class, () -> jsonWriter.writeRaw(""));
        assertThrows(JSONException.class, () -> jsonWriter.writeComma());
        assertThrows(JSONException.class, () -> jsonWriter.write0('A'));
        assertThrows(JSONException.class, () -> jsonWriter.writeDateTimeISO8601(2001, 1, 1, 12, 13, 14, 0, 0, true));
        assertThrows(JSONException.class, () -> jsonWriter.writeTimeHHMMSS8(12, 13, 14));
        assertThrows(JSONException.class, () -> jsonWriter.writeBase64(new byte[0]));
        assertThrows(JSONException.class, () -> jsonWriter.writeRaw('A'));
        assertThrows(JSONException.class, () -> jsonWriter.writeNameRaw(new byte[0], 0, 0));
        assertThrows(JSONException.class, () -> jsonWriter.writeNameRaw(new char[0]));
        assertThrows(JSONException.class, () -> jsonWriter.writeNameRaw(new char[0], 0, 0));
        assertThrows(JSONException.class, () -> jsonWriter.writeColon());
        assertThrows(JSONException.class, () -> jsonWriter.flushTo(null, null));
    }

    @Test
    public void writeDateTime19() {
        JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
        jsonWriter.writeDateTime19(2013, 5, 6, 12, 13, 14);
        assertEquals("\"2013-05-06 12:13:14\"", JSONB.toJSONString(jsonWriter.getBytes()));
    }

    @Test
    public void writeString() {
        JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
        jsonWriter.writeChar('A');
        assertEquals("\"A\"", JSONB.toJSONString(jsonWriter.getBytes()));
    }

    @Test
    public void startArray() {
        Integer[] array = new Integer[]{1, 2, 3};
        JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
        jsonWriter.startArray(array, array.length);
        for (Integer item : array) {
            jsonWriter.writeInt32(item);
        }
        assertEquals("[\n" +
                "\t1,\n" +
                "\t2,\n" +
                "\t3\n" +
                "]", JSONB.toJSONString(jsonWriter.getBytes()));
    }

    @Test
    public void capacity() throws Exception {
        Field bytes = JSONWriterJSONB.class.getDeclaredField("bytes");
        bytes.setAccessible(true);

        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            bytes.set(jsonWriter, new byte[0]);
            jsonWriter.startArray(new Object[1], 27);
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            bytes.set(jsonWriter, new byte[0]);
            jsonWriter.endObject();
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            bytes.set(jsonWriter, new byte[0]);
            jsonWriter.writeAny(null);
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            bytes.set(jsonWriter, new byte[0]);
            jsonWriter.writeString((char[]) null);
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            bytes.set(jsonWriter, new byte[1]);
            jsonWriter.writeString(new char[0]);
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            bytes.set(jsonWriter, new byte[1]);
            char[] chars = "01234567890".toCharArray();
            jsonWriter.writeString(chars, 5, 0);
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            bytes.set(jsonWriter, new byte[0]);
            jsonWriter.writeTypeName("abc");
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            bytes.set(jsonWriter, new byte[1]);
            jsonWriter.writeTypeName("abc");

            byte[] bytes1 = (byte[]) bytes.get(jsonWriter);
            bytes.set(jsonWriter, Arrays.copyOf(bytes1, 7));
            jsonWriter.writeTypeName("abc");

            bytes.set(jsonWriter, new byte[1]);
            jsonWriter.off = 0;
            jsonWriter.writeTypeName("abc");

            bytes.set(jsonWriter, new byte[0]);
            jsonWriter.setOffset(0);
            assertEquals(0, jsonWriter.getOffset());
            jsonWriter.ensureCapacity(1);
        }

        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            bytes.set(jsonWriter, new byte[0]);
            jsonWriter.writeMillis(1000);
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            bytes.set(jsonWriter, new byte[0]);
            jsonWriter.writeMillis(214700 * 3600L * 6000L);
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            bytes.set(jsonWriter, new byte[0]);
            jsonWriter.writeInt64(1);
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            bytes.set(jsonWriter, new byte[0]);
            jsonWriter.writeInt64(64);
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            bytes.set(jsonWriter, new byte[0]);
            jsonWriter.writeInt64(262143);
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            bytes.set(jsonWriter, new byte[0]);
            jsonWriter.writeInt64(Integer.MAX_VALUE);
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            bytes.set(jsonWriter, new byte[0]);
            jsonWriter.writeInt64(new long[1]);
            jsonWriter.writeInt64((long[]) null);
            jsonWriter.writeInt64((Long) null);
            jsonWriter.writeFloat((Float) null);
            jsonWriter.writeFloat((float[]) null);
            jsonWriter.writeDouble((double[]) null);
            jsonWriter.writeInt32((int[]) null);
            jsonWriter.writeInt32((Integer) null);
            jsonWriter.writeLocalDate(null);
            jsonWriter.writeLocalTime(null);
            jsonWriter.writeLocalDateTime(null);
            jsonWriter.writeZonedDateTime(null);
            jsonWriter.writeInstant(null);
            jsonWriter.writeUUID(null);
            jsonWriter.writeBigInt(null);
            jsonWriter.writeBinary(null);
            jsonWriter.writeDecimal(null);
            jsonWriter.writeBool(null);
            jsonWriter.writeBigInt(null, 0);
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            bytes.set(jsonWriter, new byte[0]);
            jsonWriter.writeInt64(new long[]{64});
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            bytes.set(jsonWriter, new byte[0]);
            jsonWriter.writeInt64(new long[]{262143});
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            bytes.set(jsonWriter, new byte[0]);
            jsonWriter.writeInt64(new long[]{Integer.MAX_VALUE});
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            bytes.set(jsonWriter, new byte[0]);
            jsonWriter.writeInt32(new int[]{8});
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            bytes.set(jsonWriter, new byte[0]);
            jsonWriter.writeInt32(new int[]{64});
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            bytes.set(jsonWriter, new byte[0]);
            jsonWriter.writeInt32(new int[]{262143});
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            bytes.set(jsonWriter, new byte[0]);
            jsonWriter.writeInt32(new int[]{Integer.MAX_VALUE});
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            bytes.set(jsonWriter, new byte[0]);
            jsonWriter.writeInt8((byte) 1);
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            bytes.set(jsonWriter, new byte[0]);
            jsonWriter.writeInt16((short) 1);
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            bytes.set(jsonWriter, new byte[0]);
            jsonWriter.writeInt32(1);
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            bytes.set(jsonWriter, new byte[0]);
            jsonWriter.writeInt32(64);
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            bytes.set(jsonWriter, new byte[0]);
            jsonWriter.writeInt32(262143);
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            bytes.set(jsonWriter, new byte[0]);
            jsonWriter.writeInt32(Integer.MAX_VALUE);
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            bytes.set(jsonWriter, new byte[0]);
            jsonWriter.writeArrayNull();
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            bytes.set(jsonWriter, new byte[0]);
            jsonWriter.writeBigInt(BigInteger.ONE);
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            bytes.set(jsonWriter, new byte[0]);
            jsonWriter.writeBool(true);
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            bytes.set(jsonWriter, new byte[0]);
            jsonWriter.writeReference("$");
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            bytes.set(jsonWriter, new byte[0]);
            jsonWriter.writeTypeName(new byte[1], 1);
            bytes.set(jsonWriter, new byte[0]);
            jsonWriter.off = 0;
            jsonWriter.writeTypeName(new byte[1], 1);
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB(JSONB.symbolTable("id"));
            bytes.set(jsonWriter, new byte[0]);
            jsonWriter.writeTypeName(new byte[1], Fnv.hashCode64("id"));
        }
    }

    @Test
    public void sizeOfInt() {
        assertEquals(5, JSONWriterJSONB.sizeOfInt(Integer.MAX_VALUE));
    }

    @Test
    public void testInstant() {
        Bean bean = new Bean();
        bean.date = Instant.ofEpochMilli(1679826319000L);

        byte[] bytes = JSONB.toBytes(bean);
        System.out.println(JSONB.toJSONString(bytes));
        Bean bean1 = JSONB.parseObject(bytes, Bean.class);
        assertEquals(bean.date.toEpochMilli(), bean1.date.toEpochMilli());
    }

    @Test
    public void writeDateTime14() {
        LocalDateTime ldt = LocalDateTime.of(2023, 3, 26, 10, 25, 19);
        JSONWriter jsonWriter = JSONWriter.ofJSONB();
        jsonWriter.writeDateTime14(
                ldt.getYear(),
                ldt.getMonthValue(),
                ldt.getDayOfMonth(),
                ldt.getHour(),
                ldt.getMinute(),
                ldt.getSecond()
        );

        byte[] bytes = jsonWriter.getBytes();
        assertEquals("\"2023-03-26 10:25:19\"", JSONB.toJSONString(bytes));
        LocalDateTime ldt1 = JSONB.parseObject(bytes, LocalDateTime.class);
        assertEquals(ldt, ldt1);
    }

    @Test
    public void writeDate8() {
        LocalDate localDate = LocalDate.of(2023, 3, 26);
        JSONWriter jsonWriter = JSONWriter.ofJSONB();
        jsonWriter.writeDateYYYMMDD8(
                localDate.getYear(),
                localDate.getMonthValue(),
                localDate.getDayOfMonth()
        );

        byte[] bytes = jsonWriter.getBytes();
        assertEquals("\"2023-03-26\"", JSONB.toJSONString(bytes));
        LocalDate localDate1 = JSONB.parseObject(bytes, LocalDate.class);
        assertEquals(localDate, localDate1);
    }

    public static class Bean {
        @JSONField(format = "yyyyMMddHHmmss")
        public Instant date;
    }

    @Test
    public void writeHex() {
        byte[] bytes = new byte[]{1, 2, 3};
        JSONWriter jsonWriter = JSONWriter.ofJSONB();
        jsonWriter.writeHex(bytes);
        byte[] jsonbBytes = jsonWriter.getBytes();
        assertArrayEquals(bytes, (byte[]) JSONB.parse(jsonbBytes));

        assertThrows(Exception.class, () -> jsonWriter.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void writeChars() {
        char[] chars = new char[256];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = (char) i;
        }
        JSONWriter jsonWriter = JSONWriter.ofJSONB();
        jsonWriter.writeString(chars);
        byte[] jsonbBytes = jsonWriter.getBytes();
        assertEquals(new String(chars), JSONB.parse(jsonbBytes));
    }

    @Test
    public void writeChars1() {
        char[] chars = new char[1024];
        Arrays.fill(chars, 'A');
        for (int i = 256; i < 768; i++) {
            chars[i] = (char) (i - 256);
        }
        JSONWriter jsonWriter = JSONWriter.ofJSONB();
        jsonWriter.writeString(chars, 256, 512);
        byte[] jsonbBytes = jsonWriter.getBytes();
        assertEquals(
                new String(chars, 256, 512),
                JSONB.parse(jsonbBytes));
    }

    @Test
    public void writeChars2() {
        char[] chars = new char[] {'A', 'A'};
        Arrays.fill(chars, 'A');
        for (int i = 128; i < 256; i++) {
            chars[1] = (char) (i - 256);
            JSONWriter jsonWriter = JSONWriter.ofJSONB();
            jsonWriter.writeString(chars, 1, 1);
            byte[] jsonbBytes = jsonWriter.getBytes();
            assertEquals(
                    new String(chars, 1, 1),
                    JSONB.parse(jsonbBytes));
        }
    }

    @Test
    public void writeChars3() {
        char[] chars = new char[128];
        for (int i = 128; i < 256; i++) {
            Arrays.fill(chars, (char) (i - 256));
            chars[0] = 'A';
            int strlen = chars.length - 1;
            JSONWriter jsonWriter = JSONWriter.ofJSONB();
            jsonWriter.writeString(chars, 1, strlen);
            byte[] jsonbBytes = jsonWriter.getBytes();
            assertEquals(
                    new String(chars, 1, strlen),
                    JSONB.parse(jsonbBytes));
        }
    }

    @Test
    public void writeCharsNull() {
        JSONWriter jsonWriter = JSONWriter.ofJSONB();
        jsonWriter.writeString((char[]) null);
        byte[] jsonbBytes = jsonWriter.getBytes();
        assertNull(JSONB.parse(jsonbBytes));
    }

    @Test
    public void writeCharsNull1() {
        JSONWriter jsonWriter = JSONWriter.ofJSONB();
        jsonWriter.writeString((char[]) null);
        byte[] jsonbBytes = jsonWriter.getBytes();
        assertNull(JSONB.parse(jsonbBytes));
    }

    @Test
    public void writeStringNull() {
        JSONWriter jsonWriter = JSONWriter.ofJSONB();
        jsonWriter.writeStringNull();
        byte[] jsonbBytes = jsonWriter.getBytes();
        assertNull(JSONB.parse(jsonbBytes));
    }

    @Test
    public void writeStringLatin1() {
        byte[] bytes = new byte[256];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) i;
        }
        JSONWriter jsonWriter = JSONWriter.ofJSONB();
        jsonWriter.writeStringLatin1(bytes);
        String str = new String(bytes, 0, bytes.length, StandardCharsets.ISO_8859_1);
        byte[] jsonbBytes = jsonWriter.getBytes();
        assertEquals(str, JSONB.parse(jsonbBytes));
    }

    @Test
    public void writeStringLatin1Pretty() {
        byte[] bytes = new byte[1024 * 128];
        Arrays.fill(bytes, (byte) '\\');
        JSONWriter jsonWriter = JSONWriter.ofJSONB();
        jsonWriter.writeStringLatin1(bytes);
        String str = new String(bytes, 0, bytes.length, StandardCharsets.ISO_8859_1);
        byte[] jsonbBytes = jsonWriter.getBytes();
        assertEquals(str, JSONB.parse(jsonbBytes));
    }

    @Test
    public void grow() {
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.writeNull();
            }
            jsonWriter.close();
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.writeReference("$.abc");
            }
            jsonWriter.close();
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.writeString(Arrays.asList("abc"));
            }
            jsonWriter.close();
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.writeStringLatin1(new byte[] {1, 2, 3});
            }
            jsonWriter.close();
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB(BrowserSecure);
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.writeStringLatin1(new byte[] {1, '>', '<'});
            }
            jsonWriter.close();
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB(BrowserSecure);
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.writeStringLatin1(new byte[] {'a', 'b', 'c'});
            }
            jsonWriter.close();
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.writeString(new char[] {'a', 'b', 'c'});
            }
            jsonWriter.close();
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.writeString(new char[] {'a', 'b', 'c'}, 0, 3);
            }
            jsonWriter.close();
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.writeUUID(UUID.randomUUID());
            }
            jsonWriter.close();
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.writeInt32(123);
            }
            jsonWriter.close();
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.writeInt8((byte) 123);
            }
            jsonWriter.close();
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.writeInt8(new byte[]{1, 2, 3});
            }
            jsonWriter.close();
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.writeInt16((short) 123);
            }
            jsonWriter.close();
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.writeInt32(new int[] {1, 2, 3});
            }
            jsonWriter.close();
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.writeListInt32(Arrays.asList(1, 2, null));
            }
            jsonWriter.close();
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.writeInt64(123L);
            }
            jsonWriter.close();
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.writeInt64(new long[] {1, 2, 3});
            }
            jsonWriter.close();
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.writeListInt64(Arrays.asList(1L, 2L, null));
            }
            jsonWriter.close();
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.writeFloat(123);
            }
            jsonWriter.close();
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.writeDouble(123L);
            }
            jsonWriter.close();
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.writeNameRaw(new byte[] {'a', 'b', 'c'});
            }
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.writeDateTime14(2014, 4, 5, 6, 7, 8);
            }
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.writeDateTime19(2014, 4, 5, 6, 7, 8);
            }
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.writeLocalDate(LocalDate.MIN);
            }
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.writeLocalDateTime(LocalDateTime.MIN);
            }
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.writeDateYYYMMDD8(2014, 4, 5);
            }
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.writeDateYYYMMDD10(2014, 4, 5);
            }
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.writeLocalTime(LocalTime.MIN);
            }
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            ZonedDateTime now = ZonedDateTime.now();
            for (int i = 0; i < 1000; i++) {
                jsonWriter.writeZonedDateTime(now);
            }
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            OffsetDateTime now = OffsetDateTime.now();
            for (int i = 0; i < 1000; i++) {
                jsonWriter.writeOffsetDateTime(now);
            }
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            OffsetTime now = OffsetTime.now();
            for (int i = 0; i < 1000; i++) {
                jsonWriter.writeOffsetTime(now);
            }
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.writeBigInt(new BigInteger("123456789012345678901234567890"), 0);
            }
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.writeDecimal(new BigDecimal("12345678901234567890.1234567890"), 0, new DecimalFormat("###.##"));
            }
        }
        {
            JSONWriterUTF16 jsonWriter = (JSONWriterUTF16) JSONWriter.ofUTF16();
            jsonWriter.chars = new char[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.write(Arrays.asList(1));
            }
        }
        {
            JSONWriterUTF16 jsonWriter = (JSONWriterUTF16) JSONWriter.ofUTF16();
            jsonWriter.chars = new char[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.write(Arrays.asList(1, 2));
            }
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.write(Arrays.asList(1, 2, 3));
            }
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.writeBool(true);
            }
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.write(JSONObject.of());
            }
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.startArray0();
            }
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.startArray1();
            }
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.startArray2();
            }
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.startArray3();
            }
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.startArray4();
            }
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.startArray4();
            }
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.startArray5();
            }
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.startArray6();
            }
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.startArray7();
            }
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.startArray8();
            }
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.startArray9();
            }
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.startArray10();
            }
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.startArray11();
            }
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.startArray12();
            }
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.startArray13();
            }
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.startArray14();
            }
        }
        {
            JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
            jsonWriter.bytes = new byte[0];
            for (int i = 0; i < 1000; i++) {
                jsonWriter.startArray15();
            }
        }
    }

    @Test
    public void println() {
        JSONWriterJSONB jsonWriter = (JSONWriterJSONB) JSONWriter.ofJSONB();
        jsonWriter.bytes = new byte[0];
        jsonWriter.println();
        assertEquals("<empty>", jsonWriter.toString());
    }

    @Test
    public void unsupport() {
        JSONWriter jsonWriter = JSONWriter.ofJSONB();
        assertThrows(JSONException.class, () -> jsonWriter.writeRaw('a', 'b'));
        JSONWriter.illegalYear(Integer.MAX_VALUE);
    }
}
