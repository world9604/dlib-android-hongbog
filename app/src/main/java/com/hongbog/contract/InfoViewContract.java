package com.hongbog.contract;

/**
 * Created by taein on 2018-07-19.
 * ViewModel이 직접 Activity를 참조하지 않도록 인터페이스로 명확히 나눈다
 */
public interface InfoViewContract {

    void showError();

    void startCameraActivity(String label);
}
