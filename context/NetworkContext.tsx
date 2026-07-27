import React, { createContext, useContext, useEffect, useRef, useState } from 'react';
import { Alert, Linking, Platform } from 'react-native';
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
  const prevConnectionType = useRef<string | null>(null);

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

      // Detect transition from 'wifi' to 'none' or 'cellular'
      if (
        prevConnectionType.current === 'wifi' &&
        currentType !== 'wifi' &&
        autoSwitchEnabled
      ) {
        Alert.alert(
          'Wi-Fi Connection Lost',
          `Auto-Switch is active. Would you like to switch mobile data to ${selectedSim}?`,
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
  }, [autoSwitchEnabled, selectedSim]);

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
    if (Platform.OS === 'android') {
      const intents = [
        'android.settings.DATA_ROAMING_SETTINGS',
        'android.settings.NETWORK_OPERATOR_SETTINGS',
        'android.settings.WIRELESS_SETTINGS',
      ];
      for (const intentUrl of intents) {
        try {
          const supported = await Linking.canOpenURL(intentUrl).catch(() => true);
          if (supported) {
            await Linking.openURL(intentUrl);
            return;
          }
        } catch (e) {
          // Continue to next fallback intent
        }
      }
      Linking.openSettings().catch(() => {});
    } else {
      Linking.openSettings().catch(() => {});
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
