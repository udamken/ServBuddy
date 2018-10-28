/************************************************************************************
 * PswGenDesktop - Manages your websites and repeatably generates passwords for them
 * PswGenDroid - Generates your passwords managed by PswGenDesktop on your mobile
 *
 *     Copyright (C) 2005-2018 Uwe Damken
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ************************************************************************************/
package de.dknapps.pswgendroid.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import de.dknapps.pswgencore.model.ServiceInfo
import de.dknapps.pswgencore.util.DomainException
import de.dknapps.pswgendroid.R
import de.dknapps.pswgendroid.adapter.PswGenAdapter
import de.dknapps.pswgendroid.model.ServiceMaintenanceViewModel
import kotlinx.android.synthetic.main.edit_service_fragment.*


class EditServiceFragment : androidx.fragment.app.Fragment() {

    companion object {

        fun newInstance() = EditServiceFragment()

    }

    private lateinit var viewModel: ServiceMaintenanceViewModel

    private val dirtyListener = object : TextWatcher {

        override fun afterTextChanged(s: Editable?) {
            viewModel.isDirty = true
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.edit_service_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        (activity!! as AppCompatActivity).supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        viewModel = ViewModelProviders.of(activity!!).get(ServiceMaintenanceViewModel::class.java)

        buttonClearService.setOnClickListener { onClickButtonClearService() }
        buttonRemoveService.setOnClickListener { onClickButtonRemoveService() }
        buttonStoreService.setOnClickListener { onClickButtonStoreService() }
        buttonUseNewPassphrase.setOnClickListener { onClickButtonUseNewPassphrase() }
    }

    override fun onResume() {
        super.onResume()
        // When the screen gets locked services are unloaded. Therefore we return to previous fragment
        // if there is currently no service selected (probably because of screen lock).
        if (viewModel.editedServiceInfo == null) {
            activity!!.supportFragmentManager.popBackStack()
        } else {
            putServiceToView(viewModel.editedServiceInfo!!)
            addAllDirtyListener()
        }
    }

    override fun onPause() {
        removeAllDirtyListener()
        viewModel.editedServiceInfo = getServiceFromView()
        super.onPause()
    }

    /**
     * Clear all fields of the currently edited service.
     */
    private fun onClickButtonClearService() {
        if (viewModel.isDirty) {
            AlertDialog.Builder(activity!!) //
                .setTitle(R.string.app_name) //
                .setMessage(R.string.DiscardChangesMsg) //
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    clearService()
                } //
                .setNegativeButton(android.R.string.cancel, null) //
                .show()
        } else {
            clearService()
        }
    }

    /**
     * Remove service with edited service abbreviation from list and store all services to file.
     */
    private fun onClickButtonRemoveService() {
        try {
            val abbreviation = serviceAbbreviation.text.toString()
            validateServiceAbbreviation(abbreviation)
            AlertDialog.Builder(activity!!) //
                .setTitle(R.string.app_name) //
                .setMessage("$abbreviation${getText(R.string.RemoveServiceMsg)}") //
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    removeServiceAnyway(abbreviation)
                } //
                .setNegativeButton(android.R.string.cancel, null) //
                .show()
        } catch (e: Exception) {
            PswGenAdapter.handleThrowable(activity!!, e)
        }
    }

    /**
     * Add edited service to list or replace it and store all services to file.
     */
    private fun onClickButtonStoreService() {
        try {
            val abbreviation = serviceAbbreviation.text.toString()
            validateServiceAbbreviation(abbreviation)
            if (viewModel.services!!.getServiceInfo(abbreviation) != null) { // service does exist?
                AlertDialog.Builder(activity!!) //
                    .setTitle(R.string.app_name) //
                    .setMessage("$abbreviation${getText(R.string.OverwriteServiceMsg)}") //
                    .setPositiveButton(android.R.string.yes) { _, _ ->
                        storeServiceAnyway()
                    } //
                    .setNegativeButton(android.R.string.no, null) //
                    .show()
            } else {
                storeServiceAnyway()
            }
        } catch (e: Exception) {
            PswGenAdapter.handleThrowable(activity!!, e)
        }
    }

    /**
     * Fill additional info with current date and change service to no longer use the old passphrase.
     */
    private fun onClickButtonUseNewPassphrase() {
        val si = getServiceFromView()
        si.resetAdditionalInfo()
        si.isUseOldPassphrase = false
        putServiceToView(si)
    }

    /**
     * Add a listener to all fields that sets a dirty tag if the content was changed.
     */
    private fun addAllDirtyListener() {
        serviceAbbreviation.addTextChangedListener(dirtyListener)
        additionalInfo.addTextChangedListener(dirtyListener)
        loginUrl.addTextChangedListener(dirtyListener)
        loginInfo.addTextChangedListener(dirtyListener)
        additionalLoginInfo.addTextChangedListener(dirtyListener)
        // useOldPassphrase: dirty tag is set in onClickButtonUseNewPassphrase()
        // lastUpdate: no dirty tag to be set, is read-only
    }

    /**
     * Remove the listener that sets a dirty tag if the content was changed from all fields.
     */
    private fun removeAllDirtyListener() {
        serviceAbbreviation.removeTextChangedListener(dirtyListener)
        additionalInfo.removeTextChangedListener(dirtyListener)
        loginUrl.removeTextChangedListener(dirtyListener)
        loginInfo.removeTextChangedListener(dirtyListener)
        additionalLoginInfo.removeTextChangedListener(dirtyListener)
        // useOldPassphrase: dirty tag is set in onClickButtonUseNewPassphrase()
        // lastUpdate: no dirty tag to be set, is read-only
    }

    /**
     * Remove service from (by marking it as deleted) regardless whether it exists or not.
     */
    private fun removeServiceAnyway(abbreviation: String) {
        try {
            viewModel.services!!.removeServiceInfo(abbreviation)
            PswGenAdapter.saveServiceInfoList(
                viewModel.servicesFile!!,
                viewModel.services!!,
                viewModel.validatedPassphrase!!
            )
            clearService()
        } catch (e: Exception) {
            PswGenAdapter.handleThrowable(activity!!, e)
        }
    }

    /**
     * Get service from view and store it regardless whether it already exists or not.
     */
    private fun storeServiceAnyway() {
        try {
            val si = getServiceFromView()
            si.isDeleted = false
            si.resetTimeMillis()
            viewModel.services!!.putServiceInfo(si)
            PswGenAdapter.saveServiceInfoList(
                viewModel.servicesFile!!,
                viewModel.services!!,
                viewModel.validatedPassphrase!!
            )
            putServiceToView(si) // update timestamp
            viewModel.isDirty = false
        } catch (e: Exception) {
            PswGenAdapter.handleThrowable(activity!!, e)
        }
    }

    /**
     * Copy values to be displayed into UI (method name identical with PswGenDesktop).
     */
    private fun putServiceToView(si: ServiceInfo) {
        serviceAbbreviation.setText(si.serviceAbbreviation)
        additionalInfo.setText(si.additionalInfo)
        loginUrl.setText(si.loginUrl)
        loginInfo.setText(si.loginInfo)
        additionalLoginInfo.setText(si.additionalLoginInfo)
        if (si.isUseOldPassphrase) {
            labelUseOldPassphrase.visibility = View.VISIBLE
            buttonStoreService.visibility = View.INVISIBLE
            buttonUseNewPassphrase.visibility = View.VISIBLE
        } else {
            labelUseOldPassphrase.visibility = View.INVISIBLE
            buttonStoreService.visibility = View.VISIBLE
            buttonUseNewPassphrase.visibility = View.INVISIBLE
        }
        lastUpdate.text = si.lastUpdate
    }

    /**
     * Return service with values from UI (method name identical with PswGenDesktop).
     */
    private fun getServiceFromView(): ServiceInfo {
        val si = ServiceInfo(serviceAbbreviation.text.toString())
        si.additionalInfo = additionalInfo.text.toString()
        si.loginUrl = loginUrl.text.toString()
        si.loginInfo = loginInfo.text.toString()
        si.additionalLoginInfo = additionalLoginInfo.text.toString()
        si.isUseOldPassphrase = labelUseOldPassphrase.visibility == View.VISIBLE
        // last update is set not set from the view but from outside
        return si
    }

    /**
     * Clear service fields and reset additional info to the current date.
     */
    private fun clearService() {
        val si = ServiceInfo()
        si.resetAdditionalInfo()
        putServiceToView(si)
        viewModel.isDirty = false
    }

    /**
     * Throws a domain exception if the given abbreviation is empty.
     */
    private fun validateServiceAbbreviation(abbreviation: String) {
        if (abbreviation.isEmpty()) {
            throw DomainException("ServiceAbbreviationEmptyMsg")
        }
    }

}
