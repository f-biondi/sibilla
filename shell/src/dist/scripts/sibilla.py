import jnius_config

jnius_config.add_classpath("lib/*")

import jnius

SibillaRuntimeAuto = jnius.autoclass("it.unicam.quasylab.sibilla.core.runtime.SibillaRuntime")

from multimethod import multimethod
import io

SimulationMonitor = jnius.autoclass("it.unicam.quasylab.sibilla.core.simulator.SimulationMonitor")
ShellSimulationMonitor = jnius.autoclass("it.unicam.quasylab.sibilla.shell.ShellSimulationMonitor")
System = jnius.autoclass("java.lang.System")
File = jnius.autoclass("java.io.File")

class SibillaRuntimeCc:
    def __init__(self):
        self.__runtime = jnius.autoclass("it.unicam.quasylab.sibilla.core.runtime.SibillaRuntime")()

    def initModules(self):
        self.__runtime.initModules()
    
    def getModules(self):
        return self.__runtime.getModules()

    def loadModule(self, name):
        self.__runtime.loadModule(name)
    
    @multimethod
    def load(self, file: File):
        self.__runtime.load(file)

    @multimethod
    def load(self, code: str):
        self.__runtime.load(code)
    
    def info(self):
        return self.__runtime.info()
    
    def setParameter(self, name: str, value: float):
        self.__runtime.setParameter(name, value)

    def getParameter(self, name: str):
        return self.__runtime.getParameter(name)

    def getParameters(self):
        return self.__runtime.getParameters()
    
    def getEvaluationEnvironment(self):
        return self.__runtime.getEvaluationEnvironment()
    
    def clear(self):
        self.__runtime.clear()

    @multimethod
    def reset(self):
        self.__runtime.reset()

    @multimethod
    def reset(self, name: str):
        self.__runtime.reset(name)

    def getInitialConfigurations(self):
        return self.__runtime.getInitialConfigurations()

    def getConfigurationInfo(self, name: str):
        return self.__runtime.getConfigurationInfo()

    def setConfiguration(self, name: str, *args: float):
        return self.__runtime.setConfiguration(name, *args)

    def checkLoadedModule(self):
        self.__runtime.checkLoadedModule()

    def getMeasures(self):
        return self.__runtime.getMeasures()

    def isEnabledMeasure(self):
        return self.__runtime.isEnabledMeasure()

    def setMeasures(self, *measures: str):
        return self.__runtime.setMeasures(*measures)

    def addMeasure(self, name: str):
        self.__runtime.addMeasure(name)

    def removeMeasure(self, name: str):
        self.__runtime.removeMeasure(name)

    def addAllMeasures(self):
        self.__runtime.addAllMeasures()

    def removeAllMeasures(self):
        self.__runtime.removeAllMeasures()

    @multimethod
    def simulate(self, monitor: SimulationMonitor, label: str):
        self.__runtime.simulate(monitor, label)

    @multimethod
    def simulate(self, label: str):
        self.__runtime.simulate(label)

    def useDescriptiveStatistics(self):
        self.__runtime.useDescriptiveStatistics()

    def useSummaryStatistics(self):
        self.__runtime.useSummaryStatistics()

    def isDescriptiveStatistics(self):
        return self.__runtime.isDescriptiveStatistics()

    def isSummaryStatistics(self):
        return self.__runtime.isSummaryStatistics()

    def getStatistics(self):
        return self.__runtime.getStatistics()

    def checkDt(self):
        self.__runtime.checkDt()

    def checkDeadline(self):
        self.__runtime.checkDeadline()

    def setDeadline(self, deadline: float):
        self.__runtime.setDeadline(deadline)

    def getDeadline(self):
        return self.__runtime.getDeadline()

    def setDt(self, dt: float):
        self.__runtime.setDt(dt)

    def getModes(self):
        return self.__runtime.getModes()

    def setMode(self, name: str):
        self.__runtime.setMode(name)

    def getMode(self):
        return self.__runtime.getMode()

    def setSeed(self, seed: int):
        self.__runtime.setSeed(seed)

    def getSeed(self):
        return self.__runtime.getSeed()

    @multimethod
    def save(self, outputFolder: str, prefix: str, postfix: str):
        return self.__runtime.save(outputFolder, prefix, postfix)

    @multimethod
    def save(self, label: str, outputFolder: str, prefix: str, postfix: str):
        return self.__runtime.save(outputFolder, prefix, postfix)

    
    def setReplica(self, replica: int):
        self.__runtime.setReplica(replica)

    def getDt(self):
        return self.__runtime.getDt()

    def getReplica(self):
        return self.__runtime.getReplica()

    def printData(label: str):
        return self.__runtime.printData(label)

class SibillaRuntimeSc:
    def __init__(self):
        self.__runtime = jnius.autoclass("it.unicam.quasylab.sibilla.core.runtime.SibillaRuntime")()

    def init_modules(self):
        self.__runtime.initModules()
    
    def get_modules(self):
        return self.__runtime.getModules()

    def load_module(self, name):
        self.__runtime.loadModule(name)
    
    @multimethod
    def load(self, file: File):
        self.__runtime.load(file)

    @multimethod
    def load(self, code: str):
        self.__runtime.load(code)
    
    def info(self):
        return self.__runtime.info()
    
    def set_parameter(self, name: str, value: float):
        self.__runtime.setParameter(name, value)

    def get_parameter(self, name: str):
        return self.__runtime.getParameter(name)

    def get_parameters(self):
        return self.__runtime.getParameters()
    
    def get_evaluation_environment(self):
        return self.__runtime.getEvaluationEnvironment()
    
    def clear(self):
        self.__runtime.clear()

    @multimethod
    def reset(self):
        self.__runtime.reset()

    @multimethod
    def reset(self, name: str):
        self.__runtime.reset(name)

    def get_initial_configurations(self):
        return self.__runtime.getInitialConfigurations()

    def get_configuration_info(self, name: str):
        return self.__runtime.getConfigurationInfo()

    def set_configuration(self, name: str, *args: float):
        return self.__runtime.setConfiguration(name, *args)

    def check_loaded_module(self):
        self.__runtime.checkLoadedModule()

    def get_measures(self):
        return self.__runtime.getMeasures()

    def is_enabled_measure(self):
        return self.__runtime.isEnabledMeasure()

    def set_measures(self, *measures: str):
        return self.__runtime.setMeasures(*measures)

    def add_measure(self, name: str):
        self.__runtime.addMeasure(name)

    def remove_measure(self, name: str):
        self.__runtime.removeMeasure(name)

    def add_all_measures(self):
        self.__runtime.addAllMeasures()

    def remove_all_measures(self):
        self.__runtime.removeAllMeasures()

    @multimethod
    def simulate(self, monitor: SimulationMonitor, label: str):
        self.__runtime.simulate(monitor, label)

    @multimethod
    def simulate(self, label: str):
        self.__runtime.simulate(label)

    def use_descriptive_statistics(self):
        self.__runtime.useDescriptiveStatistics()

    def use_summary_statistics(self):
        self.__runtime.useSummaryStatistics()

    def isDescriptiveStatistics(self):
        return self.__runtime.isDescriptiveStatistics()

    def is_summary_statistics(self):
        return self.__runtime.isSummaryStatistics()

    def get_statistics(self):
        return self.__runtime.getStatistics()

    def check_dt(self):
        self.__runtime.checkDt()

    def check_deadline(self):
        self.__runtime.checkDeadline()

    def set_deadline(self, deadline: float):
        self.__runtime.setDeadline(deadline)

    def get_deadline(self):
        return self.__runtime.getDeadline()

    def set_dt(self, dt: float):
        self.__runtime.setDt(dt)

    def get_modes(self):
        return self.__runtime.getModes()

    def set_mode(self, name: str):
        self.__runtime.setMode(name)

    def get_mode(self):
        return self.__runtime.getMode()

    def set_seed(self, seed: int):
        self.__runtime.setSeed(seed)

    def get_seed(self):
        return self.__runtime.getSeed()

    @multimethod
    def save(self, output_folder: str, prefix: str, postfix: str):
        return self.__runtime.save(output_folder, prefix, postfix)

    @multimethod
    def save(self, label: str, output_folder: str, prefix: str, postfix: str):
        return self.__runtime.save(output_folder, prefix, postfix)

    
    def set_replica(self, replica: int):
        self.__runtime.setReplica(replica)

    def get_dt(self):
        return self.__runtime.getDt()

    def get_replica(self):
        return self.__runtime.getReplica()

    def print_data(label: str):
        return self.__runtime.printData(label)

