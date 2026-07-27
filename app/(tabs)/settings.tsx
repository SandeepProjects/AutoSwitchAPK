import React from 'react';
import {
  StyleSheet,
  Text,
  View,
  ScrollView,
  TouchableOpacity,
  Switch,
  Linking,
  Platform,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { useColors } from '../../hooks/useColors';
import { useTheme, ThemePreference } from '../../context/ThemeContext';
import { useNetwork } from '../../context/NetworkContext';

export default function SettingsScreen() {
  const colors = useColors();
  const insets = useSafeAreaInsets();
  const { preference, setPreference } = useTheme();
  const { autoSwitchEnabled, setAutoSwitchEnabled, selectedSim, setSelectedSim, openMobileSettings } =
    useNetwork();

  const openDeepLink = (url: string) => {
    if (Platform.OS === 'android') {
      Linking.openURL(url).catch(() => {
        openMobileSettings();
      });
    } else {
      openMobileSettings();
    }
  };

  return (
    <ScrollView
      style={[styles.container, { backgroundColor: colors.background }]}
      contentContainerStyle={{
        paddingTop: Platform.OS === 'web' ? 40 : Math.max(insets.top + 16, 36),
        paddingBottom: Math.max(insets.bottom, 24),
        paddingHorizontal: 20,
      }}
    >
      <Text style={[styles.screenTitle, { color: colors.foreground }]}>Settings</Text>

      {/* 1. APPEARANCE */}
      <View style={styles.section}>
        <Text style={[styles.sectionHeader, { color: colors.mutedForeground }]}>APPEARANCE</Text>
        <View style={[styles.card, { backgroundColor: colors.card, borderColor: colors.border }]}>
          <View style={styles.themeSegmentContainer}>
            {(['system', 'light', 'dark'] as ThemePreference[]).map((mode) => {
              const isActive = preference === mode;
              const label = mode === 'system' ? 'Auto' : mode === 'light' ? 'Light' : 'Dark';
              const iconName =
                mode === 'system'
                  ? 'theme-light-dark'
                  : mode === 'light'
                  ? 'white-balance-sunny'
                  : 'moon-waning-crescent';

              return (
                <TouchableOpacity
                  key={mode}
                  style={[
                    styles.themeSegmentBtn,
                    isActive && { backgroundColor: colors.primary },
                  ]}
                  onPress={() => setPreference(mode)}
                  activeOpacity={0.8}
                >
                  <MaterialCommunityIcons
                    name={iconName as any}
                    size={18}
                    color={isActive ? '#FFFFFF' : colors.mutedForeground}
                  />
                  <Text
                    style={[
                      styles.themeSegmentText,
                      { color: isActive ? '#FFFFFF' : colors.foreground },
                    ]}
                  >
                    {label}
                  </Text>
                </TouchableOpacity>
              );
            })}
          </View>
        </View>
      </View>

      {/* 2. AUTOMATION */}
      <View style={styles.section}>
        <Text style={[styles.sectionHeader, { color: colors.mutedForeground }]}>AUTOMATION</Text>
        <View style={[styles.card, { backgroundColor: colors.card, borderColor: colors.border }]}>
          <View style={styles.settingRow}>
            <View style={styles.rowLeft}>
              <MaterialCommunityIcons name="swap-horizontal" size={22} color={colors.primary} />
              <View style={{ marginLeft: 12 }}>
                <Text style={[styles.settingTitle, { color: colors.foreground }]}>Auto-Switch</Text>
                <Text style={[styles.settingDesc, { color: colors.mutedForeground }]}>
                  Alert &amp; switch when Wi-Fi drops
                </Text>
              </View>
            </View>
            <Switch
              value={autoSwitchEnabled}
              onValueChange={setAutoSwitchEnabled}
              trackColor={{ false: colors.border, true: colors.primary }}
              thumbColor="#FFFFFF"
            />
          </View>
        </View>
      </View>

      {/* 3. PREFERRED DATA SIM */}
      <View style={styles.section}>
        <Text style={[styles.sectionHeader, { color: colors.mutedForeground }]}>PREFERRED DATA SIM</Text>
        <View style={[styles.card, { backgroundColor: colors.card, borderColor: colors.border }]}>
          <View style={styles.simSelectorRow}>
            {['SIM 1', 'SIM 2'].map((sim) => {
              const isSelected = selectedSim === sim;
              return (
                <TouchableOpacity
                  key={sim}
                  style={[
                    styles.simBtn,
                    { borderColor: isSelected ? colors.primary : colors.border },
                    isSelected && { backgroundColor: colors.primary + '15' },
                  ]}
                  onPress={() => setSelectedSim(sim)}
                  activeOpacity={0.8}
                >
                  <MaterialCommunityIcons
                    name="sim"
                    size={20}
                    color={isSelected ? colors.primary : colors.mutedForeground}
                  />
                  <Text
                    style={[
                      styles.simBtnText,
                      { color: isSelected ? colors.primary : colors.foreground },
                    ]}
                  >
                    {sim}
                  </Text>
                </TouchableOpacity>
              );
            })}
          </View>
        </View>
      </View>

      {/* 4. DEVICE & DEEP LINKS */}
      <View style={styles.section}>
        <Text style={[styles.sectionHeader, { color: colors.mutedForeground }]}>DEVICE SHORTCUTS</Text>
        <View style={[styles.card, { backgroundColor: colors.card, borderColor: colors.border }]}>
          <TouchableOpacity
            style={styles.actionRow}
            onPress={() => openDeepLink('android.settings.WIRELESS_SETTINGS')}
          >
            <View style={styles.rowLeft}>
              <MaterialCommunityIcons name="wifi-cog" size={22} color={colors.primary} />
              <Text style={[styles.actionText, { color: colors.foreground }]}>
                Open Wireless &amp; Network Settings
              </Text>
            </View>
            <MaterialCommunityIcons name="chevron-right" size={22} color={colors.mutedForeground} />
          </TouchableOpacity>
        </View>
      </View>

      {/* 5. TIPS & PERMISSIONS */}
      <View style={styles.section}>
        <Text style={[styles.sectionHeader, { color: colors.mutedForeground }]}>OPTIMIZATION TIPS</Text>
        <View style={[styles.card, { backgroundColor: colors.card, borderColor: colors.border }]}>
          <TouchableOpacity
            style={styles.tipItem}
            onPress={() => openDeepLink('android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS')}
          >
            <MaterialCommunityIcons name="battery-charging" size={20} color={colors.warning} />
            <Text style={[styles.tipText, { color: colors.foreground }]}>
              Unrestrict Battery Optimization
            </Text>
          </TouchableOpacity>

          <View style={[styles.divider, { backgroundColor: colors.border }]} />

          <TouchableOpacity
            style={styles.tipItem}
            onPress={() => openDeepLink('android.settings.DATA_USAGE_SETTINGS')}
          >
            <MaterialCommunityIcons name="database-sync" size={20} color={colors.accent} />
            <Text style={[styles.tipText, { color: colors.foreground }]}>
              Allow Unrestricted Background Data
            </Text>
          </TouchableOpacity>

          <View style={[styles.divider, { backgroundColor: colors.border }]} />

          <TouchableOpacity
            style={styles.tipItem}
            onPress={() => openDeepLink('android.settings.WIFI_SETTINGS')}
          >
            <MaterialCommunityIcons name="swap-vertical-bold" size={20} color={colors.primary} />
            <Text style={[styles.tipText, { color: colors.foreground }]}>
              Configure System Wi-Fi Data Switch
            </Text>
          </TouchableOpacity>
        </View>
      </View>

      {/* 6. ABOUT */}
      <View style={styles.section}>
        <Text style={[styles.sectionHeader, { color: colors.mutedForeground }]}>ABOUT</Text>
        <View style={[styles.card, { backgroundColor: colors.card, borderColor: colors.border, alignItems: 'center' }]}>
          <Text style={[styles.aboutAppName, { color: colors.foreground }]}>AutoSwitch Mobile</Text>
          <Text style={[styles.aboutVersion, { color: colors.mutedForeground }]}>Version 1.0.0 (Expo SDK 54)</Text>
        </View>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  screenTitle: {
    fontSize: 28,
    fontWeight: '700',
    marginBottom: 20,
  },
  section: {
    marginBottom: 20,
  },
  sectionHeader: {
    fontSize: 12,
    fontWeight: '700',
    letterSpacing: 0.8,
    marginBottom: 8,
    marginLeft: 4,
  },
  card: {
    borderRadius: 16,
    padding: 16,
    borderWidth: 1,
  },
  themeSegmentContainer: {
    flexDirection: 'row',
    backgroundColor: '#00000008',
    borderRadius: 12,
    padding: 4,
  },
  themeSegmentBtn: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 10,
    borderRadius: 8,
  },
  themeSegmentText: {
    fontSize: 14,
    fontWeight: '600',
    marginLeft: 6,
  },
  settingRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  rowLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
  },
  settingTitle: {
    fontSize: 16,
    fontWeight: '600',
  },
  settingDesc: {
    fontSize: 13,
    marginTop: 2,
  },
  simSelectorRow: {
    flexDirection: 'row',
    gap: 12,
  },
  simBtn: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 12,
    borderRadius: 12,
    borderWidth: 1.5,
  },
  simBtnText: {
    fontSize: 15,
    fontWeight: '700',
    marginLeft: 8,
  },
  actionRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  actionText: {
    fontSize: 15,
    fontWeight: '600',
    marginLeft: 12,
  },
  tipItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 10,
  },
  tipText: {
    fontSize: 14,
    fontWeight: '500',
    marginLeft: 12,
  },
  divider: {
    height: 1,
    marginVertical: 4,
  },
  aboutAppName: {
    fontSize: 16,
    fontWeight: '700',
  },
  aboutVersion: {
    fontSize: 13,
    marginTop: 4,
  },
});
