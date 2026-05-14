package net.abaresults.progresspath.repo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import net.abaresults.progresspath.model.Clinic
import net.abaresults.progresspath.model.Kid
import net.abaresults.progresspath.model.KidObjective
import net.abaresults.progresspath.model.ObjLevel
import net.abaresults.progresspath.model.Objective
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrgRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    private var selectedClinic: Clinic? = null
    private var selectedKid: Kid? = null
    private var selectedKidObjective: KidObjective? = null
    private var selectedObjective: Objective? = null
    private var selectedLevel: ObjLevel? = null

    private var currentReport: ByteArray? = null

    fun setSelectedClinic(clinic: Clinic) {
        selectedClinic = clinic
        selectedKid = null
        selectedKidObjective = null
        selectedObjective = null
    }

    fun requireSelectedClinic() : Clinic = selectedClinic!!

    fun getSelectedClinic() : Clinic? = selectedClinic

    fun setSelectedKid(kid: Kid) {
        selectedKid = kid
        selectedKidObjective = null
        selectedObjective = null
    }

    fun requireSelectedKid() : Kid = selectedKid!!

    fun getSelectedKid() : Kid? = selectedKid

    fun setSelectedObjective(objective: Objective, kidObjective: KidObjective? = null) {
        selectedObjective = objective
        kidObjective?.let {
            selectedKidObjective = it
        }
    }

    fun requireSelectedObjective() : Objective = selectedObjective!!

    fun requireSelectedKidObjective() : KidObjective = selectedKidObjective!!

    fun getSelectedObjective() : Objective? = selectedObjective


    // TODO - Report
    fun setCurrentReport(report: ByteArray) {
        currentReport = report
    }

    fun requireCurrentReport(): ByteArray {
        return currentReport ?: throw IllegalStateException("Report not generated yet")
    }

    fun setSelectedLevel(level: ObjLevel) {
        selectedLevel = level
    }

    fun getSelectedLevel() : ObjLevel? = selectedLevel

    fun requireSelectedLevel() : ObjLevel = selectedLevel!!

}