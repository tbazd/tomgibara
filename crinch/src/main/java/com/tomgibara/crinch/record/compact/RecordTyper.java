package com.tomgibara.crinch.record.compact;

import static com.tomgibara.crinch.record.compact.ColumnType.ALL_TYPES;
import static com.tomgibara.crinch.record.compact.ColumnType.BOOLEAN_PRIMITIVE;
import static com.tomgibara.crinch.record.compact.ColumnType.BOOLEAN_WRAPPER;
import static com.tomgibara.crinch.record.compact.ColumnType.BYTE_PRIMITIVE;
import static com.tomgibara.crinch.record.compact.ColumnType.BYTE_WRAPPER;
import static com.tomgibara.crinch.record.compact.ColumnType.CHAR_PRIMITIVE;
import static com.tomgibara.crinch.record.compact.ColumnType.CHAR_WRAPPER;
import static com.tomgibara.crinch.record.compact.ColumnType.DOUBLE_PRIMITIVE;
import static com.tomgibara.crinch.record.compact.ColumnType.DOUBLE_WRAPPER;
import static com.tomgibara.crinch.record.compact.ColumnType.FLOAT_PRIMITIVE;
import static com.tomgibara.crinch.record.compact.ColumnType.FLOAT_WRAPPER;
import static com.tomgibara.crinch.record.compact.ColumnType.INTEGRAL_TYPES;
import static com.tomgibara.crinch.record.compact.ColumnType.INT_PRIMITIVE;
import static com.tomgibara.crinch.record.compact.ColumnType.INT_WRAPPER;
import static com.tomgibara.crinch.record.compact.ColumnType.LONG_WRAPPER;
import static com.tomgibara.crinch.record.compact.ColumnType.PRIMITIVE_TYPES;
import static com.tomgibara.crinch.record.compact.ColumnType.SHORT_PRIMITIVE;
import static com.tomgibara.crinch.record.compact.ColumnType.SHORT_WRAPPER;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.tomgibara.crinch.record.LinearRecord;
import com.tomgibara.crinch.record.ValueParser;

//TODO use column parser when appropriate methods are available

//TODO support BigInteger
//TODO support BigDecimal and identify float accuracy loss
public class RecordTyper {

	private final ValueParser parser;
	
	private boolean first = true;
	private final List<Set<ColumnType>> types = new ArrayList<Set<ColumnType>>();
	//TODO use something like this for identifying enums
	private final List<String[]> values = new ArrayList<String[]>();
	
	RecordTyper(ValueParser parser) {
		this.parser = parser;
	}
	
	void type(LinearRecord r) {
		int index = 0;
		while (r.hasNext()) {
			Set<ColumnType> typeSet;
			String[] valueArr;
			if (index == types.size()) {
				typeSet = EnumSet.allOf(ColumnType.class);
				valueArr = new String[2];
				types.add(typeSet);
				values.add(valueArr);
				if (!first) typeSet.remove(PRIMITIVE_TYPES);
			} else {
				typeSet = types.get(index);
				valueArr = values.get(index);
			}
			String str = r.nextString();
			if (str == null || str.isEmpty()) {
				typeSet.removeAll(PRIMITIVE_TYPES);
			} else {
				if (typeSet.contains(LONG_WRAPPER)) {
					try {
						long value = parser.parseLong(str);
						if (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) {
							typeSet.remove(BYTE_PRIMITIVE);
							typeSet.remove(BYTE_WRAPPER);
						}
						if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
							typeSet.remove(SHORT_PRIMITIVE);
							typeSet.remove(SHORT_WRAPPER);
						}
						if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
							typeSet.remove(INT_PRIMITIVE);
							typeSet.remove(INT_WRAPPER);
						}
					} catch (IllegalArgumentException e) {
						typeSet.removeAll(INTEGRAL_TYPES);
					}
				}
				if (typeSet.contains(FLOAT_WRAPPER)) {
					try {
						parser.parseFloat(str);
					} catch (IllegalArgumentException e) {
						typeSet.remove(FLOAT_PRIMITIVE);
						typeSet.remove(FLOAT_WRAPPER);
					}
				}
				if (typeSet.contains(DOUBLE_WRAPPER)) {
					try {
						parser.parseDouble(str);
					} catch (IllegalArgumentException e) {
						typeSet.remove(DOUBLE_PRIMITIVE);
						typeSet.remove(DOUBLE_WRAPPER);
					}
				}
				if (typeSet.contains(CHAR_WRAPPER)) {
					if (str.length() > 1) {
						typeSet.remove(CHAR_PRIMITIVE);
						typeSet.remove(CHAR_WRAPPER);
					}
				}
				if (typeSet.contains(BOOLEAN_WRAPPER)) {
					try {
						parser.parseBoolean(str);
					} catch (IllegalArgumentException e) {
						typeSet.remove(BOOLEAN_PRIMITIVE);
						typeSet.remove(BOOLEAN_WRAPPER);
					}
				}
			}
			index ++;
		}
		first = false;
	}

	RecordAnalyzer analyzer() {
		//TODO use array
		return new RecordAnalyzer(parser, getColumnTypes());
	}

	List<ColumnType> getColumnTypes() {
		int size = types.size();
		List<ColumnType> list = new ArrayList<ColumnType>(size);
		for (int i = 0; i < size; i++) {
			Set<ColumnType> set = types.get(i);
			for (ColumnType type : ALL_TYPES) {
				if (set.contains(type)) {
					list.add(type);
					break;
				}
			}
		}
		return list;
	}
	
}
