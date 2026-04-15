// data/model/custom/CustomizedCharacterDto.kt
package com.example.basefragment.data.model.custom

/**
 * DTO nhẹ — chỉ lưu vào customized.json.
 * listPath KHÔNG lưu — reconstruct từ template khi cần.
 */
data class CustomizedCharacterDto(
    val id:         String                    = "",
    val templateId: String?                   = null,
    val avatar:     String                    = "",  // thumbnail để hiển thị
    val selections: ArrayList<SelectionIndex> = arrayListOf(),
    val imageSave:  String                    = "",  // path ảnh render đã lưu
    val isFlipped:  Boolean                   = false,
    val updatedAt:  Long                      = System.currentTimeMillis()
)

// Extension convert 2 chiều
fun CustomModel.toDto() = CustomizedCharacterDto(
    id         = id,
    templateId = templateId,
    avatar     = avatar,
    selections = ArrayList(selections),
    imageSave  = imageSave,
    isFlipped  = isFlipped,
    updatedAt  = updatedAt
)

fun CustomizedCharacterDto.toModel(templateListPath: ArrayList<BodyPartModel> = arrayListOf()) = CustomModel(
    id         = id,
    templateId = templateId,
    avatar     = avatar,
    listPath   = templateListPath,  // inject từ template khi load
    selections = ArrayList(selections),
    imageSave  = imageSave,
    isFlipped  = isFlipped,
    updatedAt  = updatedAt
)