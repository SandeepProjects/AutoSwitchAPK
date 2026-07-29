import React, { createContext, useContext, useEffect, useRef, useState } from 'react';
import { Alert, Linking } from 'react-native';
import NetInfo, { NetInfoState } from '@react-native-community/netinfo';
import AsyncStorage from '@react-native-async-storage/async-storage';

interface NetworkContextType {
  netState: NetInfoState | null;
  isConnected: boolean;
  connectionType: string;
  ssid: string | null;
  cellularGeneration: string | null;
  autoSwitchEnabled: boolean;
  selectedSim: string;
  setAutoSwitchEnabled: (enabled: boolean) => void;
  setSelectedSim: (sim: string) => void;
  openMobileSettings: () => void;
}

const SETTINGS_STORAGE_KEY = '@autoswitch_v2_settings';

const NetworkContext = createContext<NetworkContextType>({
  netState: null,
  isConnected: true,
  connectionType: 'wifi',
  ssid: null,
  cellularGeneration: null,
  autoSwitchEnabled: true,
  selectedSim: 'SIM 1',
  setAutoSwitchEnabled: () => {},
  setSelectedSim: () => {},
  openMobileSettings: () => {},
});

export const NetworkProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [netState, setNetState] = useState<NetInfoState | null>(null);
  const [autoSwitchEnabled, setAutoSwitchState] = useState<boolean>(true);
  const [selectedSim, setSelectedSimState] = useState<string>('SIM 1');

  const autoSwitchRef = useRef(autoSwitchEnabled);
  const selectedSimRef = useRef(selectedSim);
  const prevConnectionType = useRef<string | null>(null);

  useEffect(() => {
    autoSwitchRef.current = autoSwitchEnabled;
  }, [autoSwitchEnabled]);

  useEffect(() => {
    selectedSimRef.current = selectedSim;
  }, [selectedSim]);

  useEffect(() => {
    AsyncStorage.getItem(SETTINGS_STORAGE_KEY).then((data) => {
      if (data) {
        try {
          const parsed = JSON.parse(data);
          if (typeof parsed.autoSwitchEnabled === 'boolean') {
            setAutoSwitchState(parsed.autoSwitchEnabled);
          }
          if (typeof parsed.selectedSim === 'string') {
            setSelectedSimState(parsed.selectedSim);
          }
        } catch (e) {
          console.error('Failed to parse settings', e);
        }
      }
    });

    const unsubscribe = NetInfo.addEventListener((state) => {
      setNetState(state);
      const currentType = state.type;

      if (
        prevConnectionType.current === 'wifi' &&
        currentType !== 'wifi' &&
        autoSwitchRef.current
      ) {
        Alert.alert(
          'Wi-Fi Connection Lost',
          `Auto-Switch is active. Would you like to switch mobile data to ${selectedSimRef.current}?`,
          [
            { text: 'Dismiss', style: 'cancel' },
            {
              text: 'Open Settings',
              onPress: () => openMobileSettings(),
            },
          ]
        );
      }

      prevConnectionType.current = currentType;
    });

    return () => unsubscribe();
  }, []);

  const setAutoSwitchEnabled = (enabled: boolean) => {
    setAutoSwitchState(enabled);
    AsyncStorage.setItem(
      SETTINGS_STORAGE_KEY,
      JSON.stringify({ autoSwitchEnabled: enabled, selectedSim })
    );
  };

  const setSelectedSim = (sim: string) => {
    setSelectedSimState(sim);
    AsyncStorage.setItem(
      SETTINGS_STORAGE_KEY,
      JSON.stringify({ autoSwitchEnabled, selectedSim: sim })
    );
  };

  const openMobileSettings = async () => {
    try {
      await Linking.openSettings();
    } catch (e) {
      console.warn('Could not open system settings', e);
    }
  };

  const isConnected = netState?.isConnected ?? true;
  const connectionType = netState?.type ?? 'none';
  const ssid = netState?.details && 'ssid' in netState.details ? (netState.details.ssid as string) : null;
  const cellularGeneration =
    netState?.details && 'cellularGeneration' in netState.details
      ? (netState.details.cellularGeneration as string)
      : null;

  return (
    <NetworkContext.Provider
      value={{
        netState,
        isConnected,
        connectionType,
        ssid,
        cellularGeneration,
        autoSwitchEnabled,
        selectedSim,
        setAutoSwitchEnabled,
        setSelectedSim,
        openMobileSettings,
      }}
    >
      {children}
    </NetworkContext.Provider>
  );
};

export const useNetwork = () => useContext(NetworkContext);
