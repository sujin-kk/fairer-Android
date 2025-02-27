package com.depromeet.housekeeper.ui.signIn

import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.depromeet.housekeeper.R
import com.depromeet.housekeeper.base.BaseFragment
import com.depromeet.housekeeper.databinding.FragmentSignNameBinding
import com.depromeet.housekeeper.model.enums.InviteViewType
import com.depromeet.housekeeper.model.enums.ProfileViewType
import com.depromeet.housekeeper.model.enums.SignViewType
import com.depromeet.housekeeper.util.NavigationUtil.navigateSafe
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class SignNameFragment : BaseFragment<FragmentSignNameBinding>(R.layout.fragment_sign_name) {
    private val viewModel: SignNameViewModel by viewModels()
    private val navArgs by navArgs<SignNameFragmentArgs>()

    override fun createView(binding: FragmentSignNameBinding) {
        binding.vm = viewModel
    }

    override fun viewCreated() {
        initView()
        bindingVm()
        initListener()
        validateName()
    }

    private fun initView(){
        binding.layoutNetwork.llDisconnectedNetwork.bringToFront()
    }

    private fun bindingVm() {
        viewModel.setViewType(navArgs.viewType)
        if(viewModel.viewType.value==SignViewType.ModifyGroupName){
            viewModel.getMyTeam()
        }
        binding.viewType = viewModel.viewType.value
        binding.signNameEt.apply {
            hint = when (viewModel.viewType.value) {
                SignViewType.UserName -> getString(R.string.sign_name_hint)
                SignViewType.InviteCode -> getString(R.string.invite_code_hint)
                else -> getString(R.string.group_name_hint)

            }
        }
        lifecycleScope.launchWhenCreated {
            viewModel.hasTeam.collect {
                binding.hasTeam = it
                binding.hasTeamLayout.failedGroupNextBtn.mainFooterButton.text =
                    getString(R.string.failed_group_button_text)
            }
        }
        lifecycleScope.launchWhenCreated {
            viewModel.responseMyTeam.collect {
                if (it != null) {
                    if(viewModel.viewType.value==SignViewType.ModifyGroupName){
                        binding.signNameEt.hint = it.teamName
                    }
                }
            }
        }
        lifecycleScope.launchWhenCreated {
            viewModel.responseJoinTeam.collect {
                if (it != null) {
                    viewModel.setIsNextBtnClickable(true)
                    if (viewModel.isDynamicLink.value) {
                        findNavController().navigateSafe(R.id.action_signNameFragment_to_groupInfoFragment2)
                    } else {
                        findNavController().navigateSafe(R.id.action_signNameFragment_to_groupInfoFragment)
                    }
                }
            }
        }
        lifecycleScope.launchWhenCreated {
            viewModel.responseTeamUpdate.collect {
                viewModel.setIsNextBtnClickable(true)
                if (it != null) {
                    findNavController().navigateUp()
                    Toast.makeText(context, R.string.modify_group_toast_massage, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        lifecycleScope.launchWhenResumed {
            viewModel.errorMessage.collect {
                if (it.isNotEmpty()) {
                    Toast.makeText(this@SignNameFragment.context, it, Toast.LENGTH_LONG).show()
                    viewModel.setErrorMessage()
                }
            }
        }

        lifecycleScope.launchWhenCreated {
            viewModel.networkError.collect {
                binding.layoutNetwork.isNetworkError = it
            }
        }
    }

    private fun initListener() {
        binding.signNameHeader.defaultHeaderTitleTv.text = ""
        binding.signNameHeader.defaultHeaderBackBtn.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.signNameNextBtn.mainFooterButton.setText(R.string.sign_name_next_btn_text)
        binding.signNameNextBtn.mainFooterButton.setOnClickListener {
            if (!viewModel.isNextBtnClickable) return@setOnClickListener
            when (viewModel.viewType.value) {
                SignViewType.UserName -> {
                    viewModel.setMemberName()
                    findNavController().navigate(
                        SignNameFragmentDirections.actionSignNameFragmentToSignProfileFragment(
                            name = viewModel.inputText.value, viewType = ProfileViewType.Sign
                        )
                    )
                }
                SignViewType.GroupName -> {
                    findNavController().navigate(
                        SignNameFragmentDirections.actionSignNameFragmentToInviteFragment(
                            houseName = viewModel.inputText.value, viewType = InviteViewType.SIGN
                        )
                    )
                }
                SignViewType.InviteCode -> {
                    viewModel.joinTeam(viewModel.inputText.value)
                    viewModel.setIsNextBtnClickable(false)
                }
                SignViewType.ModifyGroupName -> {
                    viewModel.teamNameUpdate(viewModel.inputText.value)
                    viewModel.setIsNextBtnClickable(false)
                }
            }
        }
        binding.signNameBackground.setOnClickListener {
            binding.signNameEt.clearFocus()
            val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.signNameEt.windowToken, 0)
            binding.isTextChanged = false
        }
        binding.signNameClear.setOnClickListener {
            binding.signNameEt.setText(R.string.sign_name_blank)
        }
        if (navArgs.code != null) {
            if (navArgs.code == "hasTeam") {
                viewModel.setHasTeam(true)
                binding.hasTeamLayout.failedGroupNextBtn.mainFooterButton.isEnabled = true

            }
            viewModel.setInputText(navArgs.code!!)
            binding.signNameEt.setText(navArgs.code)
            binding.signNameNextBtn.mainFooterButton.isEnabled = viewModel.inputText.value != ""
            viewModel.setDynamicLink(true)

        }
        binding.hasTeamLayout.failedGroupNextBtn.mainFooterButton.setOnClickListener {
            findNavController().navigate(SignNameFragmentDirections.actionSignNameFragmentToMainFragment())
        }

        binding.signNameEt.setOnTouchListener { status, _ ->
            status.requestFocus()
            if (viewModel.inputText.value.isNotEmpty()) {
                binding.isTextChanged = true
            }
            false
        }
    }

    private fun validateName() {
        val pattern = "[0-9|a-z|A-Z|ㄱ-ㅎ|ㅏ-ㅣ|가-힝|ㆍᆢ| ]*"
        binding.signNameEt.addTextChangedListener {
            val value: String = binding.signNameEt.text.toString()
            viewModel.setInputText(binding.signNameEt.text.toString())
            binding.isTextChanged = true
            if (!value.matches(pattern.toRegex())) {
                binding.isError = true
                binding.signNameNextBtn.mainFooterButton.isEnabled = false
                binding.signNameError.setText(R.string.sign_name_error)
            } else if(value.length>16) {
                binding.isError = true
                binding.signNameNextBtn.mainFooterButton.isEnabled = false
                binding.signNameError.setText(R.string.sign_name_text_over_error)
            } else{
                binding.isError = false
                binding.signNameNextBtn.mainFooterButton.isEnabled =
                    viewModel.inputText.value != ""
            }
            if (value == "") {
                binding.isTextChanged = false
            }
        }
    }

}

