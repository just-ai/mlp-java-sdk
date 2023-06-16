package com.mlp.sdk.datatypes.taskzoo

enum class TokenPosTag {
    UNKNOWN,
    NOUN,
    ADJECTIVE_FULL,
    ADJECTIVE_SHORT,
    COMPARATIVE,
    VERB,
    INFINITIVE,
    PARTICLE_FULL,
    PARTICLE_SHORT,
    GERUND,
    NUMERICAL,
    ADVERB,
    NOUN_PRONOUN,
    PREDICATIVE,
    PREPOSITION,
    CONJUNCTION,
    PARTICLE,
    INTERJECTION,
    PUNCTUATION,
}

enum class TenseType {
    UNKNOWN,
    PAST,
    PRESENT,
    FUTURE
}


enum class CaseType {
    UNKNOWN,
    NOMINATIVE,
    GENITIVE,
    DATIVE,
    ACCUSATIVE,
    INSTRUMENTAL,
    PREPOSITIONAL,
    VOCATION,
    GENITIVE_2,
    ACCUSATIVE_2,
    PREPOSITIONAL_2,
}

enum class GenderType {
    UNKNOWN,
    MASCULINE,
    FEMININE,
    NEUTER,
}


enum class NumberType {
    UNKNOWN,
    SINGULAR,
    PLURAL,
}


enum class EntityType {
    UNKNOWN,
    PERSON,
    TOPONYM,
    LOCATION,
    ORGANIZATION,
    SURNAME,
    FIRST_NAME,
    PATRNAME,
    OBSCENE,
    LATIN_CHARS,
    INTEGER_NUMBER,
    ORDINAL_NUMBER,
    ROMNUMBER,
    CARDINAL_NUMBER,
    AMOUNT_OF_MONEY,
    QUANTITY,
    ABBREVIATION,
    DISTANCE,
    TEMPERATURE,
    VOLUME,
    TIME,
    DATE,
    DATETIME,
    TIME_DURATION,
    TIME_INTERVAL,
    PHONE_NUMBER,
    EMAIL,
    URL,
    EVENT,
    BUILDING,
    LANGUAGE,
    LAW,
    COMMUNITY,
    PERCENT,
    PRODUCT,
    WORK_OF_ART,
}


enum class SourceType {
    UNKNOWN,
    DUCKLING,
    MYSTEM,
    PYMORPHY,
    MLPS,
    SPACY,
    SLOVNET,
    DEEPPAVLOV,
}

// COMPONENTS

open class Span (
    open val start_index: Int,
    open val end_index: Int
)


open class SpanWithConfidence(
    start_index: Int,
    end_index: Int,
    val confidence: Float
): Span(start_index, end_index)


class AlignedSpans (
    val original_spans: List<Span>,
    val new_spans: List<Span>
)


class Token (
    val value: String,
    val span: Span
)


class Tokens(
    val tokens: List<Token>
)


class Item(
    val value: String
)

open class Items(
    val items: List<Item>
)


open class ScoredItems(
    items: List<Item>,
    val scores: List<Float>
): Items(items)



class EmbeddingVector(
    val vector: List<Float>
)



class ScoredLanguages(
    val languages: List<String>,
    val scores: List<Float>
)

class ScoredTextInfo(
    val text: String,
    val score: Float,
    val positive_labels: List<Item>,
    val negative_labels: List<Item>
)


class MetricClassifierScoredItems(
    items: List<Item>,
    scores: List<Float>,
    val info: List<ScoredTextInfo>
): ScoredItems(items, scores)


open class RawInformationValue(
    val value: String
)


class NamedEntity(
    value: String,
    val entity_type: String,
    val span: Span,
    val entity: String,
    val source_type: String,
): RawInformationValue(value)


class NamedEntities(
    val entities: List<NamedEntity>
)


class Texts(
    val values: List<String>
)


class ExtractedText(
    start_index: Int,
    end_index: Int,
    confidence: Float,
    val text: String
): SpanWithConfidence(start_index, end_index, confidence)


class ExtractedTexts(
    val texts: List<ExtractedText>
)



class ExtractedTextsList(
    val extracted_texts_list: List<ExtractedTexts>
)



class CaseTag(
    val case: CaseType?
)

class GenderTag(
    val gender: GenderType?
)

class NumberTag(
    val number: NumberType?
)

class PosTag(
    val pos_tag: TokenPosTag
)

class TenseTag(
    val tense: TenseType?
)

class InflectorTag(
    val case: CaseType?,
    val gender: GenderType?,
    val number: NumberType?,
    val tense: TenseType?,
)


class TokenWithRawInfo(
    value: String,
    val token: Token
): RawInformationValue(value)


class TokensWithRawInfo(
    val tokens: List<TokenWithRawInfo>,
    val source: String
)

class CorrectedInstance(
    val value: String,
    val variants: List<String>,
    val span: Span
)

class CorrectedInstances(
    val instances: List<CorrectedInstance>,
    val alignment: AlignedSpans
)

class ScoredSeq2SeqTexts(
    val similarity_scores: List<Float>?,
    val perplexity_scores: List<Float>?,
)

class DialogHistoryPair(
    val user: String,
    val bot: String
)


class Dialog(
    val user: String,
    val dialog_history: List<DialogHistoryPair>?
)



// COLLECTIONS
open class TextsCollection(
    val texts: List<String>
)


class EmbeddedTextsCollection(
    val embedded_texts: List<EmbeddingVector>
)


class TextsListCollection(
    val texts_list: List<Texts>
)



class FlagsCollection(
    val flags: List<Boolean>
)


open class ItemsCollection(
    val items_list: List<Items>
)


class DoubledItemsCollection(
    items_list: List<Items>,
    val extra_items_list: List<Items>
): ItemsCollection(items_list)


class TextsWithQueriesListCollection(
    texts: List<String>,
    val queries_list: List<Texts>,
): TextsCollection(texts)



class InflectorTextsCollection(
    texts: List<String>,
    val tags: List<InflectorTag>
): TextsCollection(texts)



class ConformerTextsCollection(
    texts: List<String>,
    val numbers: List<Int>
): TextsCollection(texts)



class ScoredItemsCollection(
    val items_list: List<ScoredItems>
)


class TextsWithScoredItemsCollection(
    val texts: List<String>,
    val items_list: List<ScoredItems>
)


class ScoredItemsWithInfoCollection(
    val items_list: List<MetricClassifierScoredItems>
)


class ScoredLanguagesCollection(
    val scored_languages_list: List<ScoredLanguages>
)


class TokenizedTextsCollection(
    val tokens_list: List<Tokens>
)


class CorrectedInstancesCollection(
    val corrected_texts: List<String>,
    val corrected_instances_list: List<CorrectedInstances>
)


class NamedEntitiesCollection(
    val entities_list: List<NamedEntities>
)


class TokensWithRawInfoCollection(
    val tokens_list: List<TokensWithRawInfo>
)


class ExtractedTextsCollection(
    val texts_list: List<ExtractedTextsList>
)


class ScoredSeq2SeqTextsListCollection(
    val texts_list: List<ScoredSeq2SeqTexts>
)

