#include "gtest/gtest.h"

#include "zserio/BitStreamWriter.h"
#include "zserio/BitStreamReader.h"

#include "identifiers/structure_name_clashing_with_java/StructureNameClashingWithJava.h"

namespace identifiers
{
namespace structure_name_clashing_with_java
{

class StructureNameClashingWithJavaTest : public ::testing::Test
{
protected:
    static const size_t BIT_SIZE;
};

const size_t StructureNameClashingWithJavaTest::BIT_SIZE =
        8 * 1 + // all auto optionals
        8 + // Byte
        16 + // Short
        32 + // Integer
        64 + // Long
        64 + // BigInteger
        32 + // Float
        64 + // Double
        8; // String '\0'

TEST_F(StructureNameClashingWithJavaTest, emptyConstructor)
{
    StructureNameClashingWithJava structureNameClashingWithJava;
    ASSERT_FALSE(structureNameClashingWithJava.getByteField().hasValue());
    ASSERT_FALSE(structureNameClashingWithJava.getShortField().hasValue());
    ASSERT_FALSE(structureNameClashingWithJava.getIntegerField().hasValue());
    ASSERT_FALSE(structureNameClashingWithJava.getLongField().hasValue());
    ASSERT_FALSE(structureNameClashingWithJava.getBigIntegerField().hasValue());
    ASSERT_FALSE(structureNameClashingWithJava.getFloatField().hasValue());
    ASSERT_FALSE(structureNameClashingWithJava.getDoubleField().hasValue());
    ASSERT_FALSE(structureNameClashingWithJava.getStringField().hasValue());
}

TEST_F(StructureNameClashingWithJavaTest, bitSizeOf)
{
    StructureNameClashingWithJava structureNameClashingWithJava{
        Byte{0},
        Short{0},
        Integer{0},
        Long{0},
        BigInteger{0},
        Float{0.0f},
        Double{0.0},
        String{""}
    };

    ASSERT_EQ(BIT_SIZE, structureNameClashingWithJava.bitSizeOf());
}

} // namespace structure_name_clashing_with_java
} // namespace identifiers
