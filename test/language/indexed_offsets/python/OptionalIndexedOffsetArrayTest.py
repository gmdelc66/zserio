import unittest
import zserio

from testutils import getZserioApi

class OptionalIndexedOffsetArrayTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.api = getZserioApi(__file__, "indexed_offsets.zs").optional_indexed_offset_array

    def testBitSizeOfWithOptional(self):
        hasOptional = True
        createWrongOffsets = False
        optionalIndexedOffsetArray = self._createOptionalIndexedOffsetArray(hasOptional, createWrongOffsets)
        self.assertEqual(OptionalIndexedOffsetArrayTest._getOptionalIndexedOffsetArrayBitSize(hasOptional),
                         optionalIndexedOffsetArray.bitSizeOf())

    def testBitSizeOfWithoutOptional(self):
        hasOptional = False
        createWrongOffsets = False
        optionalIndexedOffsetArray = self._createOptionalIndexedOffsetArray(hasOptional, createWrongOffsets)
        self.assertEqual(OptionalIndexedOffsetArrayTest._getOptionalIndexedOffsetArrayBitSize(hasOptional),
                         optionalIndexedOffsetArray.bitSizeOf())

    def testInitializeOffsetsWithOptional(self):
        hasOptional = True
        createWrongOffsets = True
        optionalIndexedOffsetArray = self._createOptionalIndexedOffsetArray(hasOptional, createWrongOffsets)
        bitPosition = 0
        self.assertEqual(OptionalIndexedOffsetArrayTest._getOptionalIndexedOffsetArrayBitSize(hasOptional),
                         optionalIndexedOffsetArray.initializeOffsets(bitPosition))
        self._checkOptionalIndexedOffsetArray(optionalIndexedOffsetArray, hasOptional)

    def testInitializeOffsetsWithoutOptional(self):
        hasOptional = False
        createWrongOffsets = False
        optionalIndexedOffsetArray = self._createOptionalIndexedOffsetArray(hasOptional, createWrongOffsets)
        bitPosition = 0
        self.assertEqual(OptionalIndexedOffsetArrayTest._getOptionalIndexedOffsetArrayBitSize(hasOptional),
                         optionalIndexedOffsetArray.initializeOffsets(bitPosition))

    def testReadWithOptional(self):
        hasOptional = True
        writeWrongOffsets = False
        writer = zserio.BitStreamWriter()
        OptionalIndexedOffsetArrayTest._writeOptionalIndexedOffsetArrayToStream(writer, hasOptional,
                                                                                writeWrongOffsets)
        optionalIndexedOffsetArray = self.api.OptionalIndexedOffsetArray()
        reader = zserio.BitStreamReader(writer.getByteArray())
        optionalIndexedOffsetArray.read(reader)
        self._checkOptionalIndexedOffsetArray(optionalIndexedOffsetArray, hasOptional)

    def testReadWithoutOptional(self):
        hasOptional = False
        writeWrongOffsets = False
        writer = zserio.BitStreamWriter()
        OptionalIndexedOffsetArrayTest._writeOptionalIndexedOffsetArrayToStream(writer, hasOptional,
                                                                                writeWrongOffsets)
        optionalIndexedOffsetArray = self.api.OptionalIndexedOffsetArray()
        reader = zserio.BitStreamReader(writer.getByteArray())
        optionalIndexedOffsetArray.read(reader)
        self._checkOptionalIndexedOffsetArray(optionalIndexedOffsetArray, hasOptional)

    def testWriteWithOptional(self):
        hasOptional = True
        createWrongOffsets = True
        optionalIndexedOffsetArray = self._createOptionalIndexedOffsetArray(hasOptional, createWrongOffsets)
        writer = zserio.BitStreamWriter()
        optionalIndexedOffsetArray.write(writer)
        self._checkOptionalIndexedOffsetArray(optionalIndexedOffsetArray, hasOptional)
        reader = zserio.BitStreamReader(writer.getByteArray())
        readOptionalIndexedOffsetArray = self.api.OptionalIndexedOffsetArray.fromReader(reader)
        self._checkOptionalIndexedOffsetArray(readOptionalIndexedOffsetArray, hasOptional)
        self.assertTrue(optionalIndexedOffsetArray == readOptionalIndexedOffsetArray)

    def testWriteWithoutOptional(self):
        hasOptional = False
        createWrongOffsets = False
        optionalIndexedOffsetArray = self._createOptionalIndexedOffsetArray(hasOptional, createWrongOffsets)
        writer = zserio.BitStreamWriter()
        optionalIndexedOffsetArray.write(writer)
        reader = zserio.BitStreamReader(writer.getByteArray())
        readOptionalIndexedOffsetArray = self.api.OptionalIndexedOffsetArray.fromReader(reader)
        self._checkOptionalIndexedOffsetArray(readOptionalIndexedOffsetArray, hasOptional)
        self.assertTrue(optionalIndexedOffsetArray == readOptionalIndexedOffsetArray)

    @staticmethod
    def _writeOptionalIndexedOffsetArrayToStream(writer, hasOptional, writeWrongOffsets):
        currentOffset = ELEMENT0_OFFSET
        for i in range(NUM_ELEMENTS):
            if writeWrongOffsets and i == NUM_ELEMENTS - 1:
                writer.writeBits(WRONG_OFFSET, 32)
            else:
                writer.writeBits(currentOffset, 32)
            currentOffset += zserio.bitsizeof.getBitSizeOfString(DATA[i]) // 8

        writer.writeBool(hasOptional)

        if hasOptional:
            writer.writeBits(0, 7)
            for i in range(NUM_ELEMENTS):
                writer.writeString(DATA[i])

        writer.writeBits(FIELD_VALUE, 6)

    def _checkOffsets(self, optionalIndexedOffsetArray, offsetShift):
        offsets = optionalIndexedOffsetArray.getOffsets()
        self.assertEqual(NUM_ELEMENTS, len(offsets))
        expectedOffset = ELEMENT0_OFFSET + offsetShift
        for i in range(NUM_ELEMENTS):
            self.assertEqual(expectedOffset, offsets[i])
            expectedOffset += zserio.bitsizeof.getBitSizeOfString(DATA[i]) // 8

    def _checkOptionalIndexedOffsetArray(self, optionalIndexedOffsetArray, hasOptional):
        offsetShift = 0
        self._checkOffsets(optionalIndexedOffsetArray, offsetShift)

        self.assertEqual(hasOptional, optionalIndexedOffsetArray.getHasOptional())

        if hasOptional:
            data = optionalIndexedOffsetArray.getData()
            self.assertEqual(NUM_ELEMENTS, len(data))
            for i in range(NUM_ELEMENTS):
                self.assertTrue(DATA[i] == data[i])

        self.assertEqual(FIELD_VALUE, optionalIndexedOffsetArray.getField())

    def _createOptionalIndexedOffsetArray(self, hasOptional, createWrongOffsets):
        optionalIndexedOffsetArray = self.api.OptionalIndexedOffsetArray()

        offsets = []
        currentOffset = ELEMENT0_OFFSET
        for i in range(NUM_ELEMENTS):
            if createWrongOffsets and i == NUM_ELEMENTS - 1:
                offsets.append(WRONG_OFFSET)
            else:
                offsets.append(currentOffset)
            currentOffset += zserio.bitsizeof.getBitSizeOfString(DATA[i]) // 8

        optionalIndexedOffsetArray.setOffsets(offsets)
        optionalIndexedOffsetArray.setHasOptional(hasOptional)

        if hasOptional:
            optionalIndexedOffsetArray.setData(DATA)

        optionalIndexedOffsetArray.setField(FIELD_VALUE)

        return optionalIndexedOffsetArray

    @staticmethod
    def _getOptionalIndexedOffsetArrayBitSize(hasOptional):
        bitSize = NUM_ELEMENTS * 32 + 1
        if hasOptional:
            bitSize += 7
            for i in range(NUM_ELEMENTS):
                bitSize += zserio.bitsizeof.getBitSizeOfString(DATA[i])
        bitSize += 6

        return bitSize

NUM_ELEMENTS = 5

WRONG_OFFSET = 0
ELEMENT0_OFFSET = NUM_ELEMENTS * 4 + 1

FIELD_VALUE = 63

DATA = ["Green", "Red", "Pink", "Blue", "Black"]
