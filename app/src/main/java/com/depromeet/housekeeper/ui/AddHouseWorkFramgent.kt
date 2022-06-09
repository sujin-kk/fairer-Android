package com.depromeet.housekeeper.ui

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.depromeet.housekeeper.R
import com.depromeet.housekeeper.adapter.AddHouseWorkChoreAdapter
import com.depromeet.housekeeper.adapter.DayRepeatAdapter
import com.depromeet.housekeeper.databinding.FragmentAddHouseWorkBinding
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import java.util.*

class AddHouseWorkFramgent : Fragment() {
    lateinit var binding: FragmentAddHouseWorkBinding
    lateinit var dayRepeatAdapter: DayRepeatAdapter
    lateinit var addHouseWorkChoreAdapter: AddHouseWorkChoreAdapter
    private val addTodo2ViewModel: AddHouseWorkViewModel by viewModels()
    private val navArgs by navArgs<AddHouseWorkFramgentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_add_house_work, container, false)
        binding.lifecycleOwner = this.viewLifecycleOwner
        binding.vm = addTodo2ViewModel
        addTodo2ViewModel.addCalendarView(navArgs.selectDate.date)
        binding.currentDate = addTodo2ViewModel.bindingDate()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindingVm()
        initListener()
        setAdapter()
    }

    private fun bindingVm() {
        val choreNames = navArgs.spaceChores.houseWorks
        val space = navArgs.spaceChores.spaceName
        addTodo2ViewModel.updateSpace(space)
        addTodo2ViewModel.setDate(navArgs.selectDate.date)
        addTodo2ViewModel.initChores(addTodo2ViewModel.getSpace(), choreNames)
        Timber.d(addTodo2ViewModel.getChores().toString())

        lifecycleScope.launchWhenStarted {
            addTodo2ViewModel.selectCalendar.collect {
                binding.addHouseWorkDateTv.text = addTodo2ViewModel.bindingDate()
            }
        }

        lifecycleScope.launchWhenCreated {
            addTodo2ViewModel.networkError.collect {
                binding.isConnectedNetwork = it
            }
        }

        lifecycleScope.launchWhenCreated {
            addTodo2ViewModel.houseWorkCreateResponse.collect {
                if (it?.any { it.success } == false) {
                    // 화면 전환
                    findNavController().navigate(R.id.action_addHouseWorkFramgent_to_mainFragment)
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initListener() {
        // header title
        binding.addHouseWorkHeader.defaultHeaderTitleTv.text = ""

        // header back button
        binding.addHouseWorkHeader.defaultHeaderBackBtn.setOnClickListener {
            it.findNavController().navigateUp()
        }

        binding.addHouseWorkDoneBtn.mainFooterButton.apply {
            text = resources.getString(R.string.add_todo_done_btn_txt)
            isEnabled = true

            setOnClickListener {
                // 마지막 position update
                updateChore(addTodo2ViewModel.getPosition(PositionType.CUR))
                addTodo2ViewModel.updateChoreDate()

                // 집안일 생성 api
                addTodo2ViewModel.createHouseWorks()
            }
        }

        binding.todoTimePicker.setOnTimeChangedListener { _, _, _ ->
            binding.addHouseWorkAllDayCheckBox.isChecked = false
            val time = binding.todoTimePicker.getDisPlayedTime()
            addTodo2ViewModel.updateTime(time.first, time.second)
        }

        binding.addHouseWorkAllDayCheckBox.apply {
            setOnClickListener {
                val time = binding.todoTimePicker.getDisPlayedTime()
                addTodo2ViewModel.updateTime(time.first, time.second)
            }
        }

        binding.addHouseWorkDateTv.setOnClickListener {
            createDatePickerDialog()
        }
    }

    private fun createDatePickerDialog() {
        val selectDate = navArgs.selectDate.date

        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectDate.split("-")[0].toInt())
            set(Calendar.MONTH, selectDate.split("-")[1].toInt())
            set(Calendar.DAY_OF_MONTH, selectDate.split("-")[2].toInt())
        }

        val datePickerDialog = DatePickerDialog(
            this.requireContext(),
            { _, year, month, dayOfMonth ->
                addTodo2ViewModel.updateCalendarView(year, month, dayOfMonth)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) - 1,
            calendar.get(Calendar.DAY_OF_MONTH),
        )
        datePickerDialog.show()
    }


    private fun setAdapter() {
        // chore list rv adapter
        addHouseWorkChoreAdapter = AddHouseWorkChoreAdapter(addTodo2ViewModel.getChores())
        binding.addHouseWorkChoreListRv.adapter = addHouseWorkChoreAdapter

        addHouseWorkChoreAdapter.setMyItemClickListener(object :
            AddHouseWorkChoreAdapter.MyItemClickListener {
            override fun onItemClick(position: Int) {
                // 현재 chore 클릭하면 이전 chore 정보 업데이트
                addTodo2ViewModel.updatePositions(position)
                val prePos = addTodo2ViewModel.getPosition(PositionType.PRE)
                updateChore(prePos)

                // 현재 chore 기준으로 뷰 업데이트
                updateView(position)
            }
        })

        addHouseWorkChoreAdapter.setMyItemRemoveListener(object :
            AddHouseWorkChoreAdapter.MyRemoveClickListener {
            override fun onRemoveClick(position: Int) {
                // 현재 select 된 pos 정보 -> select 되지 않아도 remove 가능하기 때문
                val selectedPos = addHouseWorkChoreAdapter.selectedChore.indexOf(1)

                if (addTodo2ViewModel.getPosition(PositionType.CUR) != selectedPos) {
                    addTodo2ViewModel.updatePositions(selectedPos)
                }
                updateView(addTodo2ViewModel.getPosition(PositionType.CUR))
            }

        })

        // 요일 반복 rv adapter
        val days: Array<String> = resources.getStringArray(R.array.day_array)
        dayRepeatAdapter = DayRepeatAdapter(days)
        binding.addHouseWorkRepeatRv.adapter = dayRepeatAdapter

    }

    private fun updateChore(position: Int) {
        when {
            binding.addHouseWorkAllDayCheckBox.isChecked -> addTodo2ViewModel.updateChore(
                null,
                position
            )
            else -> addTodo2ViewModel.updateChore(addTodo2ViewModel.curTime.value, position)
        }
        Timber.d(addTodo2ViewModel.chores.value.toString())
    }

    private fun updateView(position: Int) {
        val chore = addTodo2ViewModel.getChore(position)
        if (chore.scheduledTime == null) {
            binding.todoTimePicker.initDisPlayedValue()
            binding.addHouseWorkAllDayCheckBox.isChecked = true
        } else {
            val time = parseTime(chore.scheduledTime!!)
            binding.todoTimePicker.setDisPlayedValue(time.first, time.second)
        }
    }

    private fun parseTime(time: String): Pair<Int, Int> {
        val temp = time.split(":")
        val hour = temp[0].toInt()
        val min = temp[1].toInt()
        return Pair(hour, min)
    }

}