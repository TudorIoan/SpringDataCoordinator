package net.abaresults.progresspath.view.kids

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itextpdf.io.font.PdfEncodings
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import net.abaresults.progresspath.model.Kid
import net.abaresults.progresspath.model.ObjItemType
import net.abaresults.progresspath.model.UserType
import net.abaresults.progresspath.repo.KidObjectiveRepository
import net.abaresults.progresspath.repo.KidRepository
import net.abaresults.progresspath.repo.ObjectiveRepository
import net.abaresults.progresspath.repo.OrgRepository
import net.abaresults.progresspath.repo.TherapistRepository
import net.abaresults.progresspath.repo.UserRepository
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import com.itextpdf.kernel.pdf.PdfDocument as ITextPdfDocument

@HiltViewModel
class KidsViewModel @Inject constructor(
    private val kidRepo: KidRepository,
    private val objectiveRepo: ObjectiveRepository,
    private val kidObjectiveRepo: KidObjectiveRepository,
    private val orgRepo: OrgRepository,
    private val userRepo: UserRepository,
    private val therapistRepo: TherapistRepository,
    @ApplicationContext private val context: Context
)  : ViewModel() {
    private val _state = MutableLiveData<KidsState>()
        .apply { value = KidsState.Idle }
    val state: LiveData<KidsState> = _state

    private val _title = MutableLiveData<String>()
    val title: LiveData<String> = _title

    private val _userType = MutableLiveData<UserType>()
    val userType: LiveData<UserType> = _userType

    val kidsList = mutableListOf<Kid>()

    fun takeAction(action: KidsAction) {
        when (action) {
            is KidsAction.Start -> handleStart()
            is KidsAction.KidClicked -> handleKidClicked(action.kid)
            is KidsAction.RemoveKid -> handleRemoveKid(action.kid)
            is KidsAction.UpdateKidName -> handleUpdateKidName(action.kid)
            is KidsAction.GenerateKidWorksheet -> handleGenerateKidWorksheet(action.kid)
        }
    }

    private fun update(newState: KidsState) {
        _state.value = newState
    }

    private fun handleStart() {
        _title.value = orgRepo.requireSelectedClinic().name
        _userType.value = UserType.fromString(userRepo.requireUserDetails().userType)

        // Reset selected clinic to reset selected Kid and Objective
        orgRepo.setSelectedClinic(orgRepo.requireSelectedClinic())

        update(KidsState.Loading)
        viewModelScope.launch {
            // Cache therapist users for coordinator to avoid per-item Firestore calls
            if (_userType.value == UserType.COORDINATOR) {
                therapistRepo.fetchTherapistsForClinic(orgRepo.requireSelectedClinic().id)
                    .onSuccess { userRepo.therapistUsers = it }
            }

            val result = if (_userType.value == UserType.THERAPIST ) {
                kidRepo.fetchAllKidsForClinicAndTherapist(orgRepo.requireSelectedClinic().id, userRepo.requireUserDetails().ownerUid)
            } else {
                kidRepo.fetchAllKidsForClinic(orgRepo.requireSelectedClinic().id)
            }
            result.onSuccess {
                kidsList.clear()
                kidsList.addAll(it)
                update(KidsState.ContentLoaded(kidsList, UserType.fromString(userRepo.requireUserDetails().userType)))
            }.onFailure { exception ->
                update(KidsState.Error(exception.message ?: "Error"))
            }
        }
    }

    private fun handleKidClicked(kid: Kid) {
        orgRepo.setSelectedKid(kid)
        update(KidsState.GoToObjectives)
        update(KidsState.Idle)
    }

    private fun handleRemoveKid(kid: Kid) {
        update(KidsState.Loading)
        viewModelScope.launch {
            val result = kidRepo.removeKid(kid)
            result.onSuccess {
                kidsList.removeIf { it.id == kid.id }
                update(KidsState.ContentLoaded(kidsList, _userType.value!!))
            }.onFailure { exception ->
                update(KidsState.Error(exception.message ?: "Error"))
            }
        }
    }

    private fun handleUpdateKidName(kid: Kid) {
        update(KidsState.Loading)
        viewModelScope.launch {
            val result = kidRepo.updateKid(kid)
            result.onSuccess {
                val index = kidsList.indexOfFirst { it.id == kid.id }
                if (index != -1) {
                    kidsList[index] = kid
                }
                update(KidsState.ContentLoaded(kidsList, _userType.value!!))
            }.onFailure { exception ->
                update(KidsState.Error(exception.message ?: "Error"))
            }
        }
    }

    private fun handleGenerateKidWorksheet(kid: Kid) {
        update(KidsState.Loading)
        viewModelScope.launch {
            try {
                val kidObjectives = kidObjectiveRepo.getActiveKidObjectives(kid.id).getOrThrow()
                val objectives = objectiveRepo.fetchObjectivesByIds(kidObjectives.map { it.objectiveId }).getOrThrow()

                val insufficientObjectives = kidObjectives.filter { ko ->
                    val isYesNoObjective = ko.itemsList.isNotEmpty() && ko.itemsList.all { it.objItem.type == ObjItemType.YES_NO }
                    val activeCount = ko.itemsList.count { it.active && !it.mastered }
                    val hasInactive = ko.itemsList.any { !it.active && !it.mastered }
                    isYesNoObjective && activeCount < 4 && hasInactive
                }.mapNotNull { ko -> objectives.find { it.id == ko.objectiveId }?.name }

                if (insufficientObjectives.isNotEmpty()) {
                    update(KidsState.InsufficientActiveItems(insufficientObjectives))
                } else {
                    val pdfBytes = generateKidWorksheetPdf(kid)
                    orgRepo.setCurrentReport(pdfBytes)
                    update(KidsState.GoToReport(pdfBytes))
                }
            } catch (exception: Exception) {
                Log.e("KidsViewModel", "Error generating kid worksheet", exception)
                update(KidsState.Error(exception.message ?: "Error generating worksheet"))
            }
        }
    }

    private suspend fun generateKidWorksheetPdf(kid: Kid): ByteArray {
        // Get active kid objectives for this kid
        val kidObjectivesResult = kidObjectiveRepo.getActiveKidObjectives(kid.id)
        val kidObjectives = kidObjectivesResult.getOrThrow()

        // Get the actual objectives
        val objectiveIds = kidObjectives.map { it.objectiveId }
        val objectivesResult = objectiveRepo.fetchObjectivesByIds(objectiveIds)
        val objectives = objectivesResult.getOrThrow()

        val outputStream = ByteArrayOutputStream()
        val writer = PdfWriter(outputStream)
        val pdfDocument = ITextPdfDocument(writer)
        val document = Document(pdfDocument)

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val currentDate = Date()

        // Fonts - Use DejaVu fonts from assets for Unicode support
        val boldFontBytes = context.assets.open("deja_vu_sans_bold.ttf").readBytes()
        val normalFontBytes = context.assets.open("deja_vu_sans.ttf").readBytes()

        val boldFont = PdfFontFactory.createFont(boldFontBytes, PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED)
        val normalFont = PdfFontFactory.createFont(normalFontBytes, PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED)

        // Title
        val titleParagraph = Paragraph("Objectives")
            .setFont(boldFont)
            .setFontSize(24f)
            .setTextAlignment(TextAlignment.CENTER)
        document.add(titleParagraph)

        // Kid name
        val kidParagraph = Paragraph("Kid: ${kid.name}")
            .setFont(normalFont)
            .setFontSize(18f)
            .setTextAlignment(TextAlignment.CENTER)
        document.add(kidParagraph)

        // Date and Therapist info
        val dateParagraph = Paragraph("Date: ${dateFormat.format(currentDate)}")
            .setFont(normalFont)
            .setFontSize(14f)
        document.add(dateParagraph)

        val therapistParagraph = Paragraph("Therapist: ${userRepo.requireUserDetails().name}")
            .setFont(normalFont)
            .setFontSize(14f)
            .setTextAlignment(TextAlignment.RIGHT)
        document.add(therapistParagraph)

        // Create table with 3 columns: Objective, Items, Response
        val table = Table(UnitValue.createPercentArray(floatArrayOf(35f, 45f, 20f)))
        table.setWidth(UnitValue.createPercentValue(100f))

        // Table headers
        table.addHeaderCell(Cell().add(Paragraph("Objective").setFont(boldFont).setFontSize(12f)))
        table.addHeaderCell(Cell().add(Paragraph("Items").setFont(boldFont).setFontSize(12f)))
        table.addHeaderCell(Cell().add(Paragraph("Response").setFont(boldFont).setFontSize(12f)))

        // Group objectives by type and sort
        val objectivesByType = objectives
            .groupBy { it.type }
            .toSortedMap(compareBy({ it.displayName }, { it.ordinal }))

        // Add data rows grouped by objective type
        objectivesByType.forEach { (objectiveType, objectivesInType) ->
            objectivesInType.forEach { objective ->
                val kidObjective = kidObjectives.find { it.objectiveId == objective.id }
                val activeItems = kidObjective?.itemsList?.filter { it.active && !it.mastered } ?: emptyList()

                if (activeItems.isNotEmpty()) {
                    val objectiveText = "${objective.type.displayName}: ${objective.name}"

                    // Create objective cell that spans all item rows
                    val objectiveCell = Cell(activeItems.size, 1)
                        .add(Paragraph(objectiveText).setFont(normalFont).setFontSize(10f))
                    table.addCell(objectiveCell)

                    // Add item rows
                    activeItems.forEach { item ->
                        table.addCell(Cell().add(Paragraph(item.objItem.name).setFont(normalFont).setFontSize(10f)))

                        // Format Response column based on ObjItemType
                        val responseText = when (item.objItem.type) {
                            net.abaresults.progresspath.model.ObjItemType.YES_NO -> "Yes         No      "
                            net.abaresults.progresspath.model.ObjItemType.FREQUENCY -> "Frequency:"
                            net.abaresults.progresspath.model.ObjItemType.CHECKMARK -> "Checkmark:"
                            net.abaresults.progresspath.model.ObjItemType.PERCENTAGE -> "Percentage:"
                        }
                        table.addCell(Cell().add(Paragraph(responseText).setFont(normalFont).setFontSize(10f)))
                    }
                }
            }
        }

        document.add(table)
        document.close()

        return outputStream.toByteArray()
    }

}