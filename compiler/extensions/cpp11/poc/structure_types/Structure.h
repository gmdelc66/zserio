/**
 * Automatically generated by Zserio C++ extension version 1.2.0.
 */

#ifndef STRUCTURE_H
#define STRUCTURE_H

#include <zserio/BitStreamReader.h>
#include <zserio/BitStreamWriter.h>
#include <zserio/BitFieldUtil.h>
#include <zserio/CppRuntimeException.h>
#include <zserio/StringConvertUtil.h>
#include <zserio/PreWriteAction.h>
#include <zserio/OptionalHolder.h>
#include <zserio/Types.h>

#include "Array.h"
#include "String.h"


class Structure;

// force zserio::OptionalHolder<Structure> to strore value on heap because Structure is recursive
namespace zserio
{
namespace detail
{
    template <>
    struct is_optimized_in_place<Structure>
    {
        static const bool value = false;
    };
}
}

class Structure
{
public:
    Structure();
    explicit Structure(zserio::BitStreamReader& in);

    // new in cpp11
    // not that ZSERIO prefix (case-insensitive) will be disabled by compiler for all user-defined identifiers
    template <typename ZSERIO_T_array, typename ZSERIO_T_extraArray, typename ZSERIO_T_str,
                typename ZSERIO_T_recursive>
    Structure(uint32_t _size, ZSERIO_T_array&& _array, bool _hasExtra,
            const zserio::OptionalHolder<uint32_t>& _extraSize, ZSERIO_T_extraArray&& _extraArray,
            ZSERIO_T_str&& _str, ZSERIO_T_recursive&& _recursive)
    :   m_areChildrenInitialized(true), m_size(_size), m_array(std::forward<ZSERIO_T_array>(_array)),
        m_hasExtra(_hasExtra), m_extraSize(_extraSize),
        m_extraArray(std::forward<ZSERIO_T_extraArray>(_extraArray)),
        m_str(std::forward<ZSERIO_T_str>(_str)), m_recursive(std::forward<ZSERIO_T_recursive>(_recursive))
    {
    }

    Structure(Structure&&) = default;
    Structure& operator=(Structure&& other) = default;

    Structure(const Structure& other);
    Structure& operator=(const Structure& other);

    void initializeChildren();

    uint32_t getSize() const;
    void setSize(uint32_t _size);

    Array& getArray();
    const Array& getArray() const;
    void setArray(const Array& _array);
    void setArray(Array&& _array);

    bool getHasExtra() const;
    void setHasExtra(bool _hasExtra);

    zserio::OptionalHolder<uint32_t>& getExtraSize();
    const zserio::OptionalHolder<uint32_t>& getExtraSize() const;
    void setExtraSize(const zserio::OptionalHolder<uint32_t>& _extraSize);
    bool hasExtraSize() const;

    zserio::OptionalHolder<Array>& getExtraArray();
    const zserio::OptionalHolder<Array>& getExtraArray() const;
    void setExtraArray(const zserio::OptionalHolder<Array>& _extraArray);
    void setExtraArray(zserio::OptionalHolder<Array>&& _extraArray);
    bool hasExtraArray() const;

    String& getStr();
    const String& getStr() const;
    void setStr(const String& _str);
    void setStr(String&& _str);

    zserio::OptionalHolder<Structure>& getRecursive();
    const zserio::OptionalHolder<Structure>& getRecursive() const;
    void setRecursive(const zserio::OptionalHolder<Structure>& _recursive);
    void setRecursive(zserio::OptionalHolder<Structure>&& _recursive);
    bool hasRecursive() const;

    size_t bitSizeOf(size_t bitPosition = 0) const;
    size_t initializeOffsets(size_t bitPosition);

    bool operator==(const Structure& other) const;
    int hashCode() const;

    void read(zserio::BitStreamReader& in);
    void write(zserio::BitStreamWriter& out,
            zserio::PreWriteAction preWriteAction = zserio::ALL_PRE_WRITE_ACTIONS);

private:
    bool m_areChildrenInitialized;
    uint32_t m_size;
    Array m_array;
    bool m_hasExtra;
    zserio::OptionalHolder<uint32_t> m_extraSize;
    zserio::OptionalHolder<Array> m_extraArray;
    String m_str;
    zserio::OptionalHolder<Structure> m_recursive;
};

#endif // STRUCTURE_H
