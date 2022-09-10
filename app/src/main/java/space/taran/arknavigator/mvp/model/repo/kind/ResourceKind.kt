package space.taran.arknavigator.mvp.model.repo.kind

import space.taran.arklib.index.ResourceKind

typealias ResourceKind = ResourceKind
// sealed class ResourceKind(val code: KindCode) {
//    class Image : ResourceKind(KindCode.IMAGE)
//
//    class Video(
//        val height: Long? = null,
//        val width: Long? = null,
//        val duration: Long? = null
//    ) : ResourceKind(KindCode.VIDEO)
//
//    class Document(val pages: Int? = null) : ResourceKind(KindCode.DOCUMENT)
//
//    class Link(
//        val title: String? = null,
//        val description: String? = null,
//        val url: String? = null
//    ) : ResourceKind(KindCode.LINK)
//
//    class PlainText : ResourceKind(KindCode.PLAINTEXT)
//
//    class Archive : ResourceKind(KindCode.ARCHIVE)
// }

// These enums are only used to store different kind in one table in Room
// enum class KindCode {
//    IMAGE, VIDEO, DOCUMENT, LINK, PLAINTEXT, ARCHIVE
// }
typealias KindCode = space.taran.arklib.index.KindCode
// enum class MetaExtraTag {
//    DURATION, WIDTH, HEIGHT, PAGES, TITLE, DESCRIPTION, URL
// }
typealias MetaExtraTag = space.taran.arklib.index.MetaExtraTag
