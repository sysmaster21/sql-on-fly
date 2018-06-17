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
public interface ILogic {

    /**
     * Метод выполняемый в транзакции
     *
     * @throws java.lang.Exception
     */
    void run() throws Exception;

}
