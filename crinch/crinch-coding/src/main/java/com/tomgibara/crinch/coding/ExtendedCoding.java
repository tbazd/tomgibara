package com.tomgibara.crinch.coding;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.tomgibara.crinch.bits.BitReader;
import com.tomgibara.crinch.bits.BitWriter;

public class ExtendedCoding implements Coding {

	// fields
	
	private final UniversalCoding coding;

	// constructor
	
	public ExtendedCoding(UniversalCoding coding) {
		if (coding == null) throw new IllegalArgumentException("null coding");
		this.coding = coding;
	}
	
    // delegated coding methods
    
	@Override
	public int encodePositiveInt(BitWriter writer, int value) {
		return coding.encodePositiveInt(writer, value);
	}

	@Override
	public int encodePositiveLong(BitWriter writer, long value) {
		return coding.encodePositiveLong(writer, value);
	}

	@Override
	public int encodePositiveBigInt(BitWriter writer, BigInteger value) {
		return coding.encodePositiveBigInt(writer, value);
	}

	@Override
	public int decodePositiveInt(BitReader reader) {
		return coding.decodePositiveInt(reader);
	}

	@Override
	public long decodePositiveLong(BitReader reader) {
		return coding.decodePositiveLong(reader);
	}

	@Override
	public BigInteger decodePositiveBigInt(BitReader reader) {
		return coding.decodePositiveBigInt(reader);
	}
    
	// extra methods

    public int encodeSignedInt(BitWriter writer, int value) {
    	value = value > 0 ? value << 1 : 1 - (value << 1);
    	return coding.unsafeEncodePositiveInt(writer, value);
    }

    public int decodeSignedInt(BitReader reader) {
    	int value = decodePositiveInt(reader);
    	// the term ... | (value & (1 << 31) serves to restore sign bit
    	// in the special case where decoding overflows
    	// but we have enough info to reconstruct the correct value
   		return (value & 1) == 1 ? ((1 - value) >> 1) | (value & (1 << 31)) : value >>> 1;
    }
    
    public int encodeSignedLong(BitWriter writer, long value) {
    	value = value > 0L ? value << 1 : 1L - (value << 1);
    	return coding.unsafeEncodePositiveLong(writer, value);
    }

    public long decodeSignedLong(BitReader reader) {
    	long value = decodePositiveLong(reader);
    	// see comments in decodeSignedInt
   		return (value & 1L) == 1L ? ((1L - value) >> 1) | (value & (1L << 63)) : value >>> 1;
    }
    
    public int encodeSignedBigInt(BitWriter writer, BigInteger value) {
    	value = value.signum() == 1 ? value = value.shiftLeft(1) : BigInteger.ONE.subtract(value.shiftLeft(1));
    	return coding.unsafeEncodePositiveBigInt(writer, value);
    }
    
    public BigInteger decodeSignedBigInt(BitReader reader) {
    	BigInteger value = decodePositiveBigInt(reader);
    	return value.testBit(0) ? BigInteger.ONE.subtract(value).shiftRight(1) : value.shiftRight(1);
    }
    
    public int encodeDouble(BitWriter writer, double value) {
    	if (Double.isNaN(value) || Double.isInfinite(value)) throw new IllegalArgumentException();
    	long bits = Double.doubleToLongBits(value);
    	long sign = bits & 0x8000000000000000L;
    	if (sign == bits) return coding.unsafeEncodePositiveInt(writer, sign == 0L ? 1 : 2);
    	
    	long mantissa = bits & 0x000fffffffffffffL;
		if (sign == 0) {
			mantissa = (mantissa << 1) + 3L;
		} else {
			mantissa = (mantissa << 1) + 4L;
		}
		int exponent = (int) ((bits & 0x7ff0000000000000L) >> 52) - 1023;
    	return coding.unsafeEncodePositiveLong(writer, mantissa) + encodeSignedInt(writer, exponent);
    }
    
    public double decodeDouble(BitReader reader) {
    	long mantissa = decodePositiveLong(reader);
    	if (mantissa == 1L) return 0.0;
    	if (mantissa == 2L) return -0.0;
    	int exponent = decodeSignedInt(reader);
    	long bits = (exponent + 1023L) << 52;
    	if ((mantissa & 1L) == 0) {
    		bits |= 0x8000000000000000L;
    		mantissa = (mantissa - 4L) >> 1;
    	} else {
    		mantissa = (mantissa - 3L) >> 1;
    	}
    	bits |= mantissa;
    	return Double.longBitsToDouble(bits);
    }

    public int encodeDecimal(BitWriter writer, BigDecimal value) {
    	return encodeSignedInt(writer, value.scale()) + encodeSignedBigInt(writer, value.unscaledValue());
    }
    
    public BigDecimal decodeDecimal(BitReader reader) {
    	int scale = decodeSignedInt(reader);
    	BigInteger bigInt = decodeSignedBigInt(reader);
    	return new BigDecimal(bigInt, scale);
    }

}
