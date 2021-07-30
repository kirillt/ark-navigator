package space.taran.arkbrowser.mvp.presenter.adapter

import space.taran.arkbrowser.mvp.model.dao.common.Preview
import space.taran.arkbrowser.mvp.view.item.PreviewItemView

typealias PreviewClickHandler = ItemClickHandler<Preview>

class PreviewsList(
    private var previews: List<Preview>,
    handler: PreviewClickHandler)
    : ItemsClickablePresenter<Preview, PreviewItemView>(handler) {

    override fun items() = previews

    override fun updateItems(items: List<Preview>) {
        previews = items
    }

    override fun bindView(view: PreviewItemView) {
        val preview = previews[view.pos]

        if (preview.predefined != null) {
            view.setPredefined(preview.predefined)
        } else {
            view.setImage(preview.image!!)
        }
    }
}