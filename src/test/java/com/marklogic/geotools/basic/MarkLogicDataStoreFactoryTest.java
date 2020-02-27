package com.marklogic.geotools.basic;

import org.geotools.data.DataAccessFactory;
import org.geotools.data.DataStoreFactorySpi;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class MarkLogicDataStoreFactoryTest {

  @Test
  public void getDisplayName() {
    DataStoreFactorySpi dataStoreFactory = new MarkLogicDataStoreFactory();
    assertEquals("MarkLogic (Basic)", dataStoreFactory.getDisplayName());
  }

  @Test
  public void getDescription() {
    DataStoreFactorySpi dataStoreFactory = new MarkLogicDataStoreFactory();
    assertEquals("Basic MarkLogic Data Store", dataStoreFactory.getDescription());
  }

  @Test
  public void getParametersInfo() {
    DataStoreFactorySpi dataStoreFactory = new MarkLogicDataStoreFactory();
    DataAccessFactory.Param[] params = dataStoreFactory.getParametersInfo();
    for (DataAccessFactory.Param param : params) {
      assertNotNull(param.getName());
      assertNotNull(param.getDescription());
      assertNotNull(param.getLevel());
      assertNotNull(param.getTitle());
      assertNotNull(param.getType());
    }
  }

  @Test
  public void canProcess() {
    DataStoreFactorySpi dataStoreFactory = new MarkLogicDataStoreFactory();
    Map<String, Serializable> params = null;
    assertFalse(dataStoreFactory.canProcess(params));

    params = new HashMap<>();
    params.put("hostname", "localhost");
    assertFalse(dataStoreFactory.canProcess(params));

    params.put("port", "8000");
    assertFalse(dataStoreFactory.canProcess(params));

    params.put("username", "admin");
    assertFalse(dataStoreFactory.canProcess(params));

    params.put("password", "admin");
    assertTrue(dataStoreFactory.canProcess(params));

    params.put("database", "geotools-content");
    assertTrue(dataStoreFactory.canProcess(params));
  }

  @Test
  public void isAvailable() {
    DataStoreFactorySpi dataStoreFactory = new MarkLogicDataStoreFactory();
    assertTrue(dataStoreFactory.isAvailable());
    assertTrue(dataStoreFactory.isAvailable());
  }

  @Test
  public void getImplementationHints() {
    DataStoreFactorySpi dataStoreFactory = new MarkLogicDataStoreFactory();
    assertTrue(dataStoreFactory.getImplementationHints().isEmpty());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void createNewDataStore() {
    DataStoreFactorySpi dataStoreFactory = new MarkLogicDataStoreFactory();
    try {
      dataStoreFactory.createNewDataStore(new HashMap<String, Serializable>());
    } catch (IOException ex){
      fail();
    }
  }
}