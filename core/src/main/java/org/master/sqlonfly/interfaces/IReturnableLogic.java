/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.master.sqlonfly.interfaces;

/**
 * Пакет операторов, выполняемых в одной транзакции
 * @param <T>
 */
public interface IReturnableLogic<T> {

    /**
     * Метод выполняемый в транзакции
     *
     * @return результат выполнения метода
     * @throws java.lang.Exception
     */
    T run() throws Exception;

}
