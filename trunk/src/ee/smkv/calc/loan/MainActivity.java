package ee.smkv.calc.loan;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import ee.smkv.calc.loan.calculators.AnnuityCalculator;
import ee.smkv.calc.loan.calculators.DifferentiatedCalculator;
import ee.smkv.calc.loan.calculators.FixedCalculator;
import ee.smkv.calc.loan.export.Exporter;
import ee.smkv.calc.loan.model.Loan;
import ee.smkv.calc.loan.utils.*;

import java.io.File;
import java.math.BigDecimal;


public class MainActivity extends Activity implements
        AdapterView.OnItemSelectedListener,
        View.OnClickListener {
    public static final String SETTINGS_NAME = MainActivity.class.getName();
    public static StoreManager storeManager;

    public static final Calculator[] CALCULATORS = new Calculator[]{
            new AnnuityCalculator(), new DifferentiatedCalculator(), new FixedCalculator()
    };
    private static final String ZERO = "0";

    private static final int LOAN_INIT = 0;
    private static final int LOAN_INVALID = 1;
    private static final int LOAN_VALID = 2;
    private static final int LOAN_CALCULATED = 3;

    private int loanState = LOAN_INIT;

    TextView
            fixedPaymentLabel,
            periodLabel,
            periodTotalLabel,
            monthlyAmountLabel,
            monthlyAmountVText,
            amountTotalVText,
            interestTotalVText,
            periodLblYear,
            periodLblMonth;

    EditText
            amountEdit,
            interestEdit,
            fixedPaymentEdit,
            periodYearEdit,
            periodMonthEdit;

    Spinner loanTypeSpinner;

    Button
            calculateButton,
            scheduleButton,
            typeHelpButton,
            typeHelpCloseButton,
            periodYearPlusButton,
            periodYearMinusButton,
            periodMonthPlusButton,
            periodMonthMinusButton;

    ScrollView scrollView;

    ViewGroup
            resultContainer,
            periodLayout;

    Loan loan = new Loan();

    Calculator calculator;


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        init();
        setIconsToButtons();
        loadSharedPreferences();
        registerEventListeners();
    }

    private void loadSharedPreferences() {
        storeManager = new StoreManager(getSharedPreferences(SETTINGS_NAME, 0));
        storeManager.loadTextViews(amountEdit, interestEdit, fixedPaymentEdit, periodYearEdit, periodMonthEdit);
        storeManager.loadSpinners(loanTypeSpinner);
        if (periodYearEdit.getText() == null || periodYearEdit.getText().length() == 0) {
            periodYearEdit.setText(ZERO);
        }
        if (periodMonthEdit.getText() == null || periodMonthEdit.getText().length() == 0) {
            periodMonthEdit.setText(ZERO);
        }
    }


    private void init() {
        amountEdit = (EditText) findViewById(R.id.Amount);
        interestEdit = (EditText) findViewById(R.id.Interest);
        monthlyAmountVText = (TextView) findViewById(R.id.MonthlyAmount);
        amountTotalVText = (TextView) findViewById(R.id.AmountTotal);
        interestTotalVText = (TextView) findViewById(R.id.IterestTotal);
        calculateButton = (Button) findViewById(R.id.Calc);
        scrollView = (ScrollView) findViewById(R.id.ScrollView);
        loanTypeSpinner = (Spinner) findViewById(R.id.Type);
        fixedPaymentLabel = (TextView) findViewById(R.id.fixedPaymentLbl);
        periodLabel = (TextView) findViewById(R.id.periodLbl);
        fixedPaymentEdit = (EditText) findViewById(R.id.fixedPaymentText);
        periodTotalLabel = (TextView) findViewById(R.id.PeriodTotal);
        monthlyAmountLabel = (TextView) findViewById(R.id.MonthlyAmountLbl);
        scheduleButton = (Button) findViewById(R.id.ScheduleButton);
        typeHelpButton = (Button) findViewById(R.id.TypeHelp);
        typeHelpCloseButton = (Button) findViewById(R.id.TypeHelpClose);
        resultContainer = (ViewGroup) findViewById(R.id.resultContainer);
        periodLayout = (ViewGroup) findViewById(R.id.periodLayout);

        periodYearPlusButton = (Button) findViewById(R.id.periodIncYear);
        periodYearMinusButton = (Button) findViewById(R.id.periodDecYear);
        periodMonthPlusButton = (Button) findViewById(R.id.periodIncMonth);
        periodMonthMinusButton = (Button) findViewById(R.id.periodDecMonth);
        periodLblYear = (TextView) findViewById(R.id.periodLblYear);
        periodLblMonth = (TextView) findViewById(R.id.periodLblMonth);
        periodYearEdit = (EditText) findViewById(R.id.periodYear);
        periodMonthEdit = (EditText) findViewById(R.id.periodMonth);
    }

    private void setIconsToButtons() {
        calculateButton.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.calculator), null, null, null);
        scheduleButton.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.table), null, null, null);
        typeHelpButton.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.help), null, null, null);
    }


    @Override
    protected void onStop() {
        super.onStop();
        storeManager.storeTextViews(amountEdit, interestEdit, fixedPaymentEdit, periodYearEdit, periodMonthEdit);
        storeManager.storeSpinners(loanTypeSpinner);
        loanState = LOAN_INIT;
    }

    private void registerEventListeners() {
        loanTypeSpinner.setOnItemSelectedListener(this);
        calculateButton.setOnClickListener(this);
        scheduleButton.setOnClickListener(this);
        typeHelpButton.setOnClickListener(this);
        periodYearPlusButton.setOnClickListener(this);
        periodYearMinusButton.setOnClickListener(this);
        periodMonthPlusButton.setOnClickListener(this);
        periodMonthMinusButton.setOnClickListener(this);

        MyTextWatcher myTextWatcher = new MyTextWatcher() {
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                checkFixPeriod(periodYearEdit);
                checkFixPeriod(periodMonthEdit);
                scheduleButton.setEnabled(false);
                loanState = LOAN_INVALID;
            }
        };
        periodYearEdit.addTextChangedListener(myTextWatcher);
        periodMonthEdit.addTextChangedListener(myTextWatcher);
    }

    public void onClick(View view) {
        try {
            if (view == calculateButton) {
                calculate();
                if (loanState == LOAN_CALCULATED) {
                    scrollView.scrollTo(resultContainer.getLeft(), resultContainer.getTop());
                }
            } else if (view == scheduleButton) {
                showSchedule();
            } else if (view == typeHelpButton) {
                startActivity(new Intent(MainActivity.this, TypeHelpActivity.class));
            } else if (view == periodYearPlusButton) {
                periodYearEdit.setText(Integer.valueOf(ViewUtil.getIntegerValue(periodYearEdit) + 1).toString());
            } else if (view == periodYearMinusButton) {
                periodYearEdit.setText(Integer.valueOf(ViewUtil.getIntegerValue(periodYearEdit) - 1).toString());
            } else if (view == periodMonthPlusButton) {
                periodMonthEdit.setText(Integer.valueOf(ViewUtil.getIntegerValue(periodMonthEdit) + 1).toString());
            } else if (view == periodMonthMinusButton) {
                periodMonthEdit.setText(Integer.valueOf(ViewUtil.getIntegerValue(periodMonthEdit) - 1).toString());
            }
        } catch (EditTextNumberFormatException e) {
            if (e.editText == periodYearEdit) {
                periodYearEdit.setText(ZERO);
            } else if (e.editText == periodMonthEdit) {
                periodMonthEdit.setText(ZERO);
            }
        }

    }

    private void checkFixPeriod(EditText editable) {
        Integer max = editable == periodMonthEdit ? 12 : 50;
        try {
            Integer value = ViewUtil.getIntegerValue(periodYearEdit);
            if (value > max) {
                editable.setText(value.toString());
            } else if (value < 0) {
                editable.setText(ZERO);
            }
        } catch (EditTextNumberFormatException e) {
            editable.setText(ZERO);
        }
    }

    private void changeCalculatorType() {
        setTitle((String) loanTypeSpinner.getSelectedItem());
        calculator = CALCULATORS[loanTypeSpinner.getSelectedItemPosition()];
        boolean isFixedPayment = calculator instanceof FixedCalculator;
        fixedPaymentLabel.setVisibility(isFixedPayment ? View.VISIBLE : View.GONE);
        fixedPaymentEdit.setVisibility(isFixedPayment ? View.VISIBLE : View.GONE);
        periodLabel.setVisibility(isFixedPayment ? View.GONE : View.VISIBLE);
        periodLayout.setVisibility(isFixedPayment ? View.GONE : View.VISIBLE);
    }


    private void showSchedule() {
        ScheduleActivity.loan = loan;
        startActivity(new Intent(MainActivity.this, ScheduleActivity.class));
    }


    protected void showError(Exception e) {
        new ErrorDialogWrapper(this, e).show();
    }

    protected void showError(int id) {
        new ErrorDialogWrapper(this, getResources().getString(id)).show();
    }

    private void calculate() {
        try {
            loanState = LOAN_INVALID;
            loan = new Loan();
            loadLoanDataFromUI();
            if (isLoanReadyForCalculation(loan)) {
                loanState = LOAN_VALID;
                calculator.calculate(loan);
                showCalculatedData();
                scheduleButton.setEnabled(true);
                loanState = LOAN_CALCULATED;
            }
        } catch (Exception e) {
            showError(e);
        }
    }

    private void loadLoanDataFromUI() {
        boolean isFixedPayment = calculator instanceof FixedCalculator;
        try {
            loan.setLoanType(loanTypeSpinner.getSelectedItemPosition());
            loan.setAmount(ViewUtil.getBigDecimalValue(amountEdit));
            loan.setInterest(ViewUtil.getBigDecimalValue(interestEdit));



            if (isFixedPayment) {
                loan.setFixedPayment(ViewUtil.getBigDecimalValue(fixedPaymentEdit));
            } else {
                int months = ViewUtil.getIntegerValue(periodMonthEdit);
                int years = ViewUtil.getIntegerValue(periodYearEdit);
                loan.setPeriod(years * 12 + months);
            }

            if(loan.getAmount().compareTo(BigDecimal.ZERO) <= 0){
                showError(R.string.errorAmount);
            }else if(loan.getInterest().compareTo(BigDecimal.ZERO) <= 0){
                showError(R.string.errorInterest);
            }else if( !isFixedPayment && loan.getPeriod() <= 0){
                showError(R.string.errorPeriod);
            }else if( isFixedPayment && loan.getFixedPayment().compareTo(BigDecimal.ZERO) <= 0){
                showError(R.string.errorFixedAmount);
            }

        } catch (EditTextNumberFormatException e) {
            if (e.editText == amountEdit) {
                showError(R.string.errorAmount);
            } else if (e.editText == interestEdit) {
                showError(R.string.errorInterest);
            } else if (e.editText == fixedPaymentEdit) {
                showError(R.string.errorFixedAmount);
            }
        }
    }

    private void showCalculatedData() {
        String monthlyPayment = "";
        BigDecimal max = loan.getMaxMonthlyPayment();
        BigDecimal min = loan.getMinMonthlyPayment();
        if (max.compareTo(min) == 0) {
            monthlyPayment = max.setScale(2, Calculator.MODE).toPlainString();
        } else {
            monthlyPayment = max.setScale(2, Calculator.MODE).toPlainString() + " - " + min.setScale(2, Calculator.MODE).toPlainString();
        }
        monthlyAmountVText.setText(monthlyPayment);

        amountTotalVText.setText(loan.getTotalAmount().setScale(2, Calculator.MODE).toPlainString());
        interestTotalVText.setText(loan.getTotalInterests().setScale(2, Calculator.MODE).toPlainString());
        periodTotalLabel.setText(loan.getPeriod().toString());

        Toast.makeText(this, getResources().getText(R.string.msgCalculated), Toast.LENGTH_SHORT).show();
    }

    private boolean isLoanReadyForCalculation(Loan loan) {
        if (loan.getAmount() == null || loan.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        if (loan.getInterest() == null || loan.getInterest().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        boolean isFixedPayment = calculator instanceof FixedCalculator;
        if (isFixedPayment && (loan.getFixedPayment() == null || loan.getFixedPayment().compareTo(BigDecimal.ZERO) <= 0)) {
            return false;
        }

        if (!isFixedPayment && (loan.getPeriod() == null || loan.getPeriod() <= 0)) {
            return false;
        }
        return true;
    }


    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        changeCalculatorType();
        loanState = LOAN_INVALID;
        if (isLoanReadyForCalculation(loan)) {
            loanState = LOAN_VALID;
            calculate();
        }
    }

    public void onNothingSelected(AdapterView<?> adapterView) {
        //ignore
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {

        if (loanState < LOAN_CALCULATED && item.getItemId() != R.id.viewCompareMenu) {
            DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (i == DialogInterface.BUTTON_POSITIVE) {
                        calculate();
                    }
                    if (loanState == LOAN_CALCULATED) {
                        menuAction(item);
                    }
                }
            };

            new AlertDialog.Builder(this).setMessage(getResources().getString(R.string.recalculateLoanQuestion))
                    .setCancelable(false)
                    .setPositiveButton(getResources().getString(R.string.yes), onClickListener)
                    .setNegativeButton(getResources().getString(R.string.no), onClickListener).create().show();

        } else {
            menuAction(item);
        }

        return true;
    }

    private boolean menuAction(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.addToCompareMenu:
                if (loan != null) {
                    storeManager.addLoan(loan);
                    openCompareActivity();
                }
                break;
            case R.id.viewCompareMenu:
                openCompareActivity();
                break;
            case R.id.exportEmailMenu:
                Exporter.sendToEmail(loan, getResources(), this);
                break;
            case R.id.exportExcelMenu:
                File file = Exporter.exportToCSVFile(loan, getResources());
                new OkDialogWrapper(this, getResources().getString(R.string.fileCreated) + ' ' + file.getName()).show();
                break;

        }
        return true;
    }


    private void openCompareActivity() {
        startActivity(new Intent(MainActivity.this, CompareActivity.class));
    }
}