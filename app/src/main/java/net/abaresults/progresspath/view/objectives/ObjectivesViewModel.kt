package net.abaresults.progresspath.view.objectives

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
import net.abaresults.progresspath.model.KidObjective
import net.abaresults.progresspath.model.ObjItemType
import net.abaresults.progresspath.model.Objective
import net.abaresults.progresspath.model.UserType
import net.abaresults.progresspath.repo.KidObjectiveRepository
import net.abaresults.progresspath.repo.ObjectiveRepository
import net.abaresults.progresspath.repo.OrgRepository
import net.abaresults.progresspath.repo.UserRepository
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import com.itextpdf.kernel.pdf.PdfDocument as ITextPdfDocument

@HiltViewModel
class ObjectivesViewModel @Inject constructor(
    private val objectiveRepo: ObjectiveRepository,
    private val kidObjectiveRepo: KidObjectiveRepository,
    private val orgRepo: OrgRepository,
    private val userRepo: UserRepository,
    @ApplicationContext private val context: Context
)  : ViewModel() {
    private val _state = MutableLiveData<ObjectivesState>()
        .apply { value = ObjectivesState.Idle }
    val state: LiveData<ObjectivesState> = _state

    private val _title = MutableLiveData<String>()
    val title: LiveData<String> = _title

    private val _userType = MutableLiveData<UserType>()
    val userType: LiveData<UserType> = _userType

    val objectives = mutableListOf<Objective>()
    val kidObjectives = mutableListOf<KidObjective>()
    val objectivesListItems = mutableListOf<ObjectivesListItem>()

    fun takeAction(action: ObjectivesAction) {
        when (action) {
            is ObjectivesAction.Start -> handleStart()
            is ObjectivesAction.ObjectiveClicked -> handleObjectiveClicked(action.objectiveListItem)
            is ObjectivesAction.ToggleObjectiveActive -> handleToggleObjectiveActive(action.kidObjective, action.isActive)
            is ObjectivesAction.RemoveObjective -> handleRemoveObjective(action.kidObjective)
            is ObjectivesAction.GenerateObjectiveReport -> handleGenerateObjectiveReport(action.kidObjective)
        }
    }

    private fun update(newState: ObjectivesState) {
        _state.value = newState
    }

    private fun handleStart() {
        _title.value = "${orgRepo.requireSelectedClinic().name} > ${orgRepo.requireSelectedKid().name}"
        _userType.value = UserType.COORDINATOR

        // Set selected kid to reset selected Objective
        orgRepo.setSelectedKid(orgRepo.requireSelectedKid())

        update(ObjectivesState.Loading)

        objectives.clear()
        kidObjectives.clear()
        objectivesListItems.clear()

        viewModelScope.launch {
            val kidId = orgRepo.requireSelectedKid().id
            
            Log.d("ObjectivesViewModel", "Coordinator objective load for kid ID: $kidId")
            
            val kidObjectivesResult = kidObjectiveRepo.getKidObjectives(kidId)
            
            kidObjectivesResult.onSuccess { kidObjectivesList ->
                val filteredKidObjectives = kidObjectivesList

                Log.d("ObjectivesViewModel", "Found ${filteredKidObjectives.size} kid objectives")
                kidObjectives.addAll(filteredKidObjectives)

                if (filteredKidObjectives.isNotEmpty()) {
                    // Get the actual objectives
                    val objectiveIds = filteredKidObjectives.map { it.objectiveId }
                    Log.d("ObjectivesViewModel", "Fetching objectives with IDs: $objectiveIds")
                    val objectivesResult = objectiveRepo.fetchObjectivesByIds(objectiveIds)
                    
                    objectivesResult.onSuccess { objectivesList ->
                        Log.d("ObjectivesViewModel", "Found ${objectivesList.size} objectives")
                        objectives.addAll(objectivesList)
                        val objectiveTypes = objectivesList.map { it.type }.toSet()
                        // Expand all objective types by default
                        objectiveTypes.forEach { objectiveType ->
                            objectivesListItems.add(ObjectivesListItem.ObjectiveTypeItem(objectiveType, expanded = false))
                        }
                        update(ObjectivesState.ContentLoaded(objectivesListItems))
                    }.onFailure { exception ->
                        Log.e("ObjectivesViewModel", "Error fetching objectives", exception)
                        update(ObjectivesState.Error(exception.message ?: "Error fetching objectives"))
                    }
                } else {
                    update(ObjectivesState.ContentLoaded(objectivesListItems))
                }
            }.onFailure { exception ->
                update(ObjectivesState.Error(exception.message ?: "Error fetching kid objectives"))
            }
        }
    }

    private fun handleObjectiveClicked(objectivesListItem: ObjectivesListItem) {
        when (objectivesListItem) {
            is ObjectivesListItem.ObjectiveTypeItem -> {
                var itemIndex = objectivesListItems.indexOfFirst { it == objectivesListItem }
                objectivesListItems[itemIndex] = objectivesListItem.copy(expanded = !objectivesListItem.expanded)
                if ((objectivesListItems[itemIndex] as ObjectivesListItem.ObjectiveTypeItem).expanded) {
                    objectives.filter { it.type == objectivesListItem.objectiveType }.forEach { objective ->
                        itemIndex ++
                        val kidObjective = kidObjectives.find { it.objectiveId == objective.id }
                        if (kidObjective != null) {
                            objectivesListItems.add(itemIndex, ObjectivesListItem.ObjectiveItem(kidObjective, objective.name, objective.type))
                        }
                    }
                } else {
                    objectivesListItems.removeAll { it is ObjectivesListItem.ObjectiveItem && it.objectiveType == objectivesListItem.objectiveType }
                }
                update(ObjectivesState.ContentLoaded(objectivesListItems))
            }
            is ObjectivesListItem.ObjectiveItem -> {
                val objective = objectives.find { it.id == objectivesListItem.kidObjective.objectiveId }!!
                orgRepo.setSelectedObjective(objective, objectivesListItem.kidObjective)
                update(ObjectivesState.GoToItems)
                update(ObjectivesState.Idle)
            }
        }
    }

    private fun handleToggleObjectiveActive(kidObjective: KidObjective, isActive: Boolean) {
        viewModelScope.launch {
            // Update existing kid objective
            val updatedKidObjective = kidObjective.copy(active = isActive)
            val result = kidObjectiveRepo.updateKidObjective(updatedKidObjective)

            result.onSuccess {
                // Update the local kidObjectives list
                val index = kidObjectives.indexOf(kidObjective)
                if (index != -1) {
                    kidObjectives[index] = updatedKidObjective
                }

                // Update the corresponding item in objectivesListItems
                updateObjectivesListItemForKidObjective(updatedKidObjective)

                // Update UI
                update(ObjectivesState.ContentLoaded(objectivesListItems))
            }.onFailure { exception ->
                Log.e("ObjectivesViewModel", "Error updating objective", exception)
                update(ObjectivesState.Error(exception.message ?: "Error updating objective"))
            }

        }
    }

    private fun updateObjectivesListItemForKidObjective(kidObjective: KidObjective) {
        // Find and update the specific objective item in objectivesListItems
        val objectiveItemIndex = objectivesListItems.indexOfFirst {
            it is ObjectivesListItem.ObjectiveItem && it.kidObjective.id == kidObjective.id
        }

        if (objectiveItemIndex != -1) {
            val currentItem = objectivesListItems[objectiveItemIndex] as ObjectivesListItem.ObjectiveItem
            val updatedItem = currentItem.copy(kidObjective = kidObjective)
            objectivesListItems[objectiveItemIndex] = updatedItem
        }
    }

    private fun handleRemoveObjective(kidObjectiveToRemove: KidObjective) {
        update(ObjectivesState.Loading)
        viewModelScope.launch {
            val result = kidObjectiveRepo.removeKidObjective(kidObjectiveToRemove.id)
            result.onSuccess {
                // Find the objective for type checking
                val objective = objectives.find { it.id == kidObjectiveToRemove.objectiveId }

                // Remove from local lists
                objectives.removeIf { it.id == kidObjectiveToRemove.objectiveId }
                kidObjectives.removeIf { it.id == kidObjectiveToRemove.id }
                objectivesListItems.removeIf {
                    it is ObjectivesListItem.ObjectiveItem && it.kidObjective.id == kidObjectiveToRemove.id
                }

                // If no more objectives of this type, remove the type header too
                objective?.let { obj ->
                    val hasMoreOfType = objectives.any { it.type == obj.type }
                    if (!hasMoreOfType) {
                        objectivesListItems.removeIf {
                            it is ObjectivesListItem.ObjectiveTypeItem && it.objectiveType == obj.type
                        }
                    }
                }

                update(ObjectivesState.ContentLoaded(objectivesListItems))
            }.onFailure { exception ->
                update(ObjectivesState.Error(exception.message ?: "Error"))
            }
        }
    }

    private fun handleGenerateObjectiveReport(kidObjective: KidObjective) {
        update(ObjectivesState.Loading)
        viewModelScope.launch {
            try {
                val objective = objectives.find { it.id == kidObjective.objectiveId }!!
                val pdfBytes = generatePdfReport(kidObjective, objective)
                orgRepo.setCurrentReport(pdfBytes)
                update(ObjectivesState.GoToReport(pdfBytes))
            } catch (exception: Exception) {
                update(ObjectivesState.Error(exception.message ?: "Error generating report"))
            }
        }
    }

    private fun generatePdfReport(kidObjective: KidObjective, objective: Objective): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val writer = PdfWriter(outputStream)
        val pdfDocument = ITextPdfDocument(writer)
        val document = Document(pdfDocument)

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        // Fonts - Use DejaVu fonts from assets for Unicode support
        val boldFontBytes = context.assets.open("deja_vu_sans_bold.ttf").readBytes()
        val normalFontBytes = context.assets.open("deja_vu_sans.ttf").readBytes()

        val boldFont = PdfFontFactory.createFont(boldFontBytes, PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED)
        val normalFont = PdfFontFactory.createFont(normalFontBytes, PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED)

        // Title
        val titleParagraph = Paragraph("Objective Report: ${objective.name}")
            .setFont(boldFont)
            .setFontSize(20f)
            .setTextAlignment(TextAlignment.CENTER)
        document.add(titleParagraph)

        // Info section
        val infoParagraph = Paragraph()
            .add("Ability: ${objective.type.displayName}\n")
            .add("Kid: ${orgRepo.requireSelectedKid().name}\n")
            .add("Clinic: ${orgRepo.requireSelectedClinic().name}")
            .setFont(normalFont)
            .setFontSize(14f)
        document.add(infoParagraph)

        // Create table with 4 columns: No, Item, First response on, Mastered on
        val table = Table(UnitValue.createPercentArray(floatArrayOf(10f, 40f, 25f, 25f)))
        table.setWidth(UnitValue.createPercentValue(100f))

        // Table headers
        table.addHeaderCell(Cell().add(Paragraph("No").setFont(boldFont).setFontSize(12f)))
        table.addHeaderCell(Cell().add(Paragraph("Item").setFont(boldFont).setFontSize(12f)))
        table.addHeaderCell(Cell().add(Paragraph("First response on").setFont(boldFont).setFontSize(12f)))
        table.addHeaderCell(Cell().add(Paragraph("Mastered on").setFont(boldFont).setFontSize(12f)))

        // Add data rows
        kidObjective.itemsList.forEachIndexed { index, item ->
            val itemNumber = (index + 1).toString()
            val itemName = item.objItem.name
            val firstResponseText = item.firstResponseTime?.let { dateFormat.format(it) } ?: "Not started"
            var masteredText = if (item.mastered) {
                item.lastResponseTime?.let { dateFormat.format(it) } ?: "by me"
            } else {
                "Not mastered"
            }

            // Mark items mastered by coordinator
            if (item.mastered && item.lastModificationByUserId == userRepo.requireUserDetails().ownerUid) {
                masteredText += " (by me)"
            }

            // Determine font style based on mastered status
            val itemFont = if (item.mastered) boldFont else normalFont

            table.addCell(Cell().add(Paragraph(itemNumber).setFont(itemFont).setFontSize(10f)))
            table.addCell(Cell().add(Paragraph(itemName).setFont(itemFont).setFontSize(10f)))
            table.addCell(Cell().add(Paragraph(firstResponseText).setFont(itemFont).setFontSize(10f)))

            // Add checkmark for mastered items
            val masteredCell = if (item.mastered) {
                Cell().add(Paragraph("$masteredText ✓").setFont(boldFont).setFontSize(10f))
            } else {
                Cell().add(Paragraph(masteredText).setFont(normalFont).setFontSize(10f))
            }
            table.addCell(masteredCell)
        }

        document.add(table)
        document.close()

        return outputStream.toByteArray()
    }

}
