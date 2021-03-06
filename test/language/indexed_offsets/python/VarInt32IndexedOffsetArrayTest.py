import unittest
import zserio

from testutils import getZserioApi

class VarInt32IndexedOffsetArrayTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.api = getZserioApi(__file__, "indexed_offsets.zs").varint32_indexed_offset_array

    def testBitSizeOf(self):
        createWrongOffsets = False
        varInt32IndexedOffsetArray = self._createVarInt32IndexedOffsetArray(createWrongOffsets)
        self.assertEqual(VarInt32IndexedOffsetArrayTest._getVarInt32IndexedOffsetArrayBitSize(),
                         varInt32IndexedOffsetArray.bitSizeOf())

    def testBitSizeOfWithPosition(self):
        createWrongOffsets = False
        varInt32IndexedOffsetArray = self._createVarInt32IndexedOffsetArray(createWrongOffsets)
        bitPosition = 1
        self.assertEqual(VarInt32IndexedOffsetArrayTest._getVarInt32IndexedOffsetArrayBitSize() - bitPosition,
                         varInt32IndexedOffsetArray.bitSizeOf(bitPosition))

    def testInitializeOffsets(self):
        createWrongOffsets = True
        varInt32IndexedOffsetArray = self._createVarInt32IndexedOffsetArray(createWrongOffsets)
        bitPosition = 0
        self.assertEqual(VarInt32IndexedOffsetArrayTest._getVarInt32IndexedOffsetArrayBitSize(),
                         varInt32IndexedOffsetArray.initializeOffsets(bitPosition))
        self._checkVarInt32IndexedOffsetArray(varInt32IndexedOffsetArray)

    def testInitializeOffsetsWithPosition(self):
        createWrongOffsets = True
        varInt32IndexedOffsetArray = self._createVarInt32IndexedOffsetArray(createWrongOffsets)
        bitPosition = 9
        self.assertEqual(VarInt32IndexedOffsetArrayTest._getVarInt32IndexedOffsetArrayBitSize() + bitPosition -
                         1, varInt32IndexedOffsetArray.initializeOffsets(bitPosition))

        offsetShift = 1
        self._checkOffsets(varInt32IndexedOffsetArray, offsetShift)

    def testRead(self):
        writeWrongOffsets = False
        writer = zserio.BitStreamWriter()
        VarInt32IndexedOffsetArrayTest._writeVarInt32IndexedOffsetArrayToStream(writer, writeWrongOffsets)
        reader = zserio.BitStreamReader(writer.getByteArray())
        varInt32IndexedOffsetArray = self.api.VarInt32IndexedOffsetArray()
        varInt32IndexedOffsetArray.read(reader)
        self._checkVarInt32IndexedOffsetArray(varInt32IndexedOffsetArray)

    def testReadWrongOffsets(self):
        writeWrongOffsets = True
        writer = zserio.BitStreamWriter()
        VarInt32IndexedOffsetArrayTest._writeVarInt32IndexedOffsetArrayToStream(writer, writeWrongOffsets)
        reader = zserio.BitStreamReader(writer.getByteArray())
        varInt32IndexedOffsetArray = self.api.VarInt32IndexedOffsetArray()
        with self.assertRaises(zserio.PythonRuntimeException):
            varInt32IndexedOffsetArray.read(reader)

    def testWrite(self):
        createWrongOffsets = True
        varInt32IndexedOffsetArray = self._createVarInt32IndexedOffsetArray(createWrongOffsets)
        writer = zserio.BitStreamWriter()
        varInt32IndexedOffsetArray.write(writer)
        self._checkVarInt32IndexedOffsetArray(varInt32IndexedOffsetArray)
        reader = zserio.BitStreamReader(writer.getByteArray())
        readVarInt32IndexedOffsetArray = self.api.VarInt32IndexedOffsetArray.fromReader(reader)
        self._checkVarInt32IndexedOffsetArray(readVarInt32IndexedOffsetArray)
        self.assertTrue(varInt32IndexedOffsetArray == readVarInt32IndexedOffsetArray)

    def testWriteWithPosition(self):
        createWrongOffsets = True
        varInt32IndexedOffsetArray = self._createVarInt32IndexedOffsetArray(createWrongOffsets)
        writer = zserio.BitStreamWriter()
        bitPosition = 8
        writer.writeBits(0, bitPosition)
        varInt32IndexedOffsetArray.write(writer)

        offsetShift = 1
        self._checkOffsets(varInt32IndexedOffsetArray, offsetShift)

    def testWriteWrongOffsets(self):
        createWrongOffsets = True
        varInt32IndexedOffsetArray = self._createVarInt32IndexedOffsetArray(createWrongOffsets)
        writer = zserio.BitStreamWriter()
        with self.assertRaises(zserio.PythonRuntimeException):
            varInt32IndexedOffsetArray.write(writer, callInitializeOffsets=False)

    @staticmethod
    def _writeVarInt32IndexedOffsetArrayToStream(writer, writeWrongOffsets):
        currentOffset = ELEMENT0_OFFSET
        for i in range(NUM_ELEMENTS):
            if writeWrongOffsets and i == NUM_ELEMENTS - 1:
                writer.writeBits(WRONG_OFFSET, 32)
            else:
                writer.writeBits(currentOffset, 32)
            currentOffset += zserio.bitsizeof.getBitSizeOfVarInt32(i) // 8

        writer.writeBits(SPACER_VALUE, 1)

        writer.writeBits(0, 7)
        for i in range(NUM_ELEMENTS):
            writer.writeVarInt32(i)

    def _checkOffsets(self, varInt32IndexedOffsetArray, offsetShift):
        offsets = varInt32IndexedOffsetArray.getOffsets()
        self.assertEqual(NUM_ELEMENTS, len(offsets))
        expectedOffset = ELEMENT0_OFFSET + offsetShift
        for i in range(NUM_ELEMENTS):
            self.assertEqual(expectedOffset, offsets[i])
            expectedOffset += zserio.bitsizeof.getBitSizeOfVarInt32(i) // 8

    def _checkVarInt32IndexedOffsetArray(self, varInt32IndexedOffsetArray):
        offsetShift = 0
        self._checkOffsets(varInt32IndexedOffsetArray, offsetShift)

        self.assertEqual(SPACER_VALUE, varInt32IndexedOffsetArray.getSpacer())

        data = varInt32IndexedOffsetArray.getData()
        self.assertEqual(NUM_ELEMENTS, len(data))
        for i in range(NUM_ELEMENTS):
            self.assertEqual(i, data[i])

    def _createVarInt32IndexedOffsetArray(self, createWrongOffsets):
        offsets = []
        currentOffset = ELEMENT0_OFFSET
        for i in range(NUM_ELEMENTS):
            if createWrongOffsets and i == NUM_ELEMENTS - 1:
                offsets.append(WRONG_OFFSET)
            else:
                offsets.append(currentOffset)
            currentOffset += zserio.bitsizeof.getBitSizeOfVarInt32(i) // 8

        data = []
        for i in range(NUM_ELEMENTS):
            data.append(i)

        return self.api.VarInt32IndexedOffsetArray.fromFields(offsets, SPACER_VALUE, data)

    @staticmethod
    def _getVarInt32IndexedOffsetArrayBitSize():
        bitSize = ELEMENT0_OFFSET * 8
        for i in range(NUM_ELEMENTS):
            bitSize += zserio.bitsizeof.getBitSizeOfVarInt32(i)

        return bitSize

NUM_ELEMENTS = 5

WRONG_OFFSET = 0

ELEMENT0_OFFSET = NUM_ELEMENTS * 4 + 1
SPACER_VALUE = 1
