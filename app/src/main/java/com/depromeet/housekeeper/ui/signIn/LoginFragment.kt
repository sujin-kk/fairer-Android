package com.depromeet.housekeeper.ui.signIn

import android.content.Intent
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.depromeet.housekeeper.R
import com.depromeet.housekeeper.base.BaseFragment
import com.depromeet.housekeeper.databinding.FragmentLoginBinding
import com.depromeet.housekeeper.model.response.LoginResponse
import com.depromeet.housekeeper.model.enums.SignViewType
import com.depromeet.housekeeper.util.PREFS_USER_NAME_DEFAULT
import com.depromeet.housekeeper.util.PrefsManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class LoginFragment : BaseFragment<FragmentLoginBinding>(R.layout.fragment_login) {
    private val RC_SIGN_IN = 1
    lateinit var mGoogleSignInClient: GoogleSignInClient
    private val viewModel: LoginViewModel by viewModels()
    private val navArgs by navArgs<LoginFragmentArgs>()

    override fun createView(binding: FragmentLoginBinding) {
        initGoogleLogin()
    }

    override fun viewCreated() {
        initView()
        bindingVM()
        initListener()
    }

    private fun initView(){
        binding.layoutNetwork.llDisconnectedNetwork.bringToFront()
    }

    private fun initListener() {
        binding.signInButton.setOnClickListener {
            signIn()
        }
    }

    override fun onStart() {
        super.onStart()
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        if (navArgs.code != "null") {
            navigateDynamicLink()
        } else if (account != null) {
            navigateOnStart()
        }
    }

    private fun initGoogleLogin() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(
                Scope("https://www.googleapis.com/auth/userinfo.email"),
                Scope("https://www.googleapis.com/auth/userinfo.profile"),
                Scope("openid")
            )
            .requestServerAuthCode(getString(R.string.server_client_id))
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(binding.root.context, gso)
    }

    private fun signIn() {
        val signInIntent: Intent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val authCode = account.serverAuthCode
                if (authCode != null) {
                    PrefsManager.setAuthCode(authCode)
                    viewModel.requestLogin()
                }
                Timber.d("!! $authCode")
            } catch (e: ApiException) {
                Timber.w("failed $e")
            }
        }
    }

    private fun bindingVM() {
        viewModel.viewModelScope.launch {
            viewModel.response.collect { response ->
                Timber.d("accesstoken:${response?.accessToken}, refreshtoken:${response?.refreshToken}")
                Timber.d("isNewMember : ${response?.isNewMember}, team: ${response?.hasTeam}, MemberName: ${response?.memberName}")
                response?.run {
                    PrefsManager.setTokens(response.accessToken, response.refreshToken)

                    // set fcm token
                    viewModel.saveToken()

                    response.memberName?.let {
                        PrefsManager.setUserName(it)
                    }
                    PrefsManager.setMemberId(this.memberId)
                    // this.memberId

                    PrefsManager.setHasTeam(response.hasTeam)
                    PrefsManager.setMemberId(response.memberId)
                    initNavigation(response)
                }
            }
        }

        lifecycleScope.launchWhenCreated {
            viewModel.networkError.collect {
                binding.layoutNetwork.isNetworkError = it
            }
        }
    }

    private fun initNavigation(response: LoginResponse) {
        when (response.hasTeam) {
            true -> {
                findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToMainFragment())
            }
            false -> {
                if (response.isNewMember) {
                    findNavController().navigate(
                        LoginFragmentDirections.actionLoginFragmentToSignNameFragment(
                            SignViewType.UserName,
                            null
                        )
                    )
                } else {
                    findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToJoinGroupFragment())
                }
            }
        }
    }

    private fun navigateDynamicLink() {
        if (PrefsManager.refreshToken != "") {
            if (PrefsManager.hasTeam) {
                findNavController().navigate(
                    LoginFragmentDirections.actionLoginFragmentToSignNameFragment(
                        SignViewType.InviteCode,
                        code = "hasTeam"
                    )
                )
            } else {
                if (PrefsManager.userName == PREFS_USER_NAME_DEFAULT) {
                    findNavController().navigate(
                        LoginFragmentDirections.actionLoginFragmentToSignNameFragment(
                            SignViewType.UserName,
                            null
                        )
                    )
                } else {
                    findNavController().navigate(
                        LoginFragmentDirections.actionLoginFragmentToSignNameFragment(
                            SignViewType.InviteCode,
                            navArgs.code
                        )
                    )
                }
            }
        }

    }

    private fun navigateOnStart() {
        if (PrefsManager.hasTeam) {
            findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToMainFragment())
        } else {
            if (PrefsManager.userName == PREFS_USER_NAME_DEFAULT) {
                findNavController().navigate(
                    LoginFragmentDirections.actionLoginFragmentToSignNameFragment(
                        SignViewType.UserName,
                        null
                    )
                )
            } else {
                findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToJoinGroupFragment())
            }
        }

    }

}