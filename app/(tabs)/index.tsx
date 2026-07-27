import React, { useEffect } from 'react';
import {
  StyleSheet,
  Text,
  View,
  TouchableOpacity,
  useWindowDimensions,
  Platform,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withRepeat,
  withTiming,
  withDelay,
  Easing,
} from 'react-native-reanimated';
import { useColors } from '../../hooks/useColors';
import { useNetwork } from '../../context/NetworkContext';

function PulseRing({ delay, color, orbSize }: { delay: number; color: string; orbSize: number }) {
  const scale = useSharedValue(1);
  const opacity = useSharedValue(0.6);

  useEffect(() => {
    scale.value = withDelay(
      delay,
      withRepeat(
        withTiming(1.5, { duration: 2400, easing: Easing.out(Easing.ease) }),
        -1,
        false
      )
    );
    opacity.value = withDelay(
      delay,
      withRepeat(
        withTiming(0, { duration: 2400, easing: Easing.out(Easing.ease) }),
        -1,
        false
      )
    );
  }, [delay, scale, opacity]);

  const animatedStyle = useAnimatedStyle(() => ({
    transform: [{ scale: scale.value }],
    opacity: opacity.value,
  }));

  return (
    <Animated.View
      style={[
        StyleSheet.absoluteFillObject,
        {
          borderRadius: orbSize / 2,
          borderWidth: 2,
          borderColor: color,
        },
        animatedStyle,
      ]}
    />
  );
}

export default function HomeScreen() {
  const colors = useColors();
  const insets = useSafeAreaInsets();
  const { width } = useWindowDimensions();
  const { isConnected, connectionType, ssid, autoSwitchEnabled, selectedSim, openMobileSettings } =
    useNetwork();

  const isTablet = width >= 600;
  const hPad = Math.round(width * 0.06);
  const orbSize = Math.min(Math.round(width * (isTablet ? 0.28 : 0.45)), 180);

  const isWifi = connectionType === 'wifi';
  const isCellular = connectionType === 'cellular';

  // Determine state colors
  const statusColor = isWifi
    ? colors.success
    : isCellular
    ? colors.primary
    : colors.destructive;

  const orbBgColor = isWifi
    ? '#E6F9F0'
    : isCellular
    ? '#E0F2FE'
    : '#FEE2E2';

  const titleText = isWifi
    ? ssid || 'Wi-Fi Network'
    : isCellular
    ? 'Mobile Data'
    : 'No Connection';

  const subtitleText = isWifi
    ? 'Connected via Wi-Fi'
    : isCellular
    ? `Connected via ${selectedSim}`
    : 'Offline — no internet';

  return (
    <View
      style={[
        styles.container,
        {
          backgroundColor: colors.background,
          paddingTop: Platform.OS === 'web' ? 40 : Math.max(insets.top, 20),
          paddingBottom: Math.max(insets.bottom, 16),
          paddingHorizontal: hPad,
        },
      ]}
    >
      {/* 1. Header Row */}
      <View style={styles.headerRow}>
        <View>
          <Text style={[styles.appTitle, { color: colors.foreground }]}>AutoSwitch</Text>
          <Text style={[styles.appSubtitle, { color: colors.mutedForeground }]}>
            Network Monitor
          </Text>
        </View>

        <View style={styles.activeBadge}>
          <View style={styles.badgeDot} />
          <Text style={styles.badgeText}>Active</Text>
        </View>
      </View>

      {/* 2. Status Orb Area */}
      <View style={styles.orbSection}>
        <View style={{ width: orbSize, height: orbSize, alignItems: 'center', justifyContent: 'center' }}>
          <PulseRing delay={0} color={statusColor} orbSize={orbSize} />
          <PulseRing delay={800} color={statusColor} orbSize={orbSize} />
          <View
            style={[
              styles.orbCircle,
              {
                width: orbSize * 0.75,
                height: orbSize * 0.75,
                borderRadius: (orbSize * 0.75) / 2,
                backgroundColor: orbBgColor,
              },
            ]}
          >
            <MaterialCommunityIcons
              name={isWifi ? 'wifi' : isCellular ? 'cellphone-wireless' : 'wifi-off'}
              size={orbSize * 0.38}
              color={statusColor}
            />
          </View>
        </View>

        <Text style={[styles.connectionTitle, { color: colors.foreground }]}>
          {titleText}
        </Text>
        <Text style={[styles.connectionSubtitle, { color: colors.mutedForeground }]}>
          {subtitleText}
        </Text>
      </View>

      {/* 3. Info Card */}
      <View style={[styles.card, { backgroundColor: colors.card, borderColor: colors.border }]}>
        {/* Row 1: Auto-Switch */}
        <View style={styles.cardRow}>
          <View style={styles.cardRowLeft}>
            <View style={[styles.iconContainer, { backgroundColor: '#E0F2FE' }]}>
              <MaterialCommunityIcons name="swap-horizontal" size={20} color={colors.primary} />
            </View>
            <Text style={[styles.cardLabel, { color: colors.foreground }]}>Auto-Switch</Text>
          </View>
          <Text style={[styles.cardValue, { color: colors.success }]}>
            {autoSwitchEnabled ? `On · ${selectedSim}` : 'Off'}
          </Text>
        </View>

        <View style={[styles.divider, { backgroundColor: colors.border }]} />

        {/* Row 2: Internet */}
        <View style={styles.cardRow}>
          <View style={styles.cardRowLeft}>
            <View style={[styles.iconContainer, { backgroundColor: '#E6F9F0' }]}>
              <MaterialCommunityIcons name="check-circle-outline" size={20} color={colors.success} />
            </View>
            <Text style={[styles.cardLabel, { color: colors.foreground }]}>Internet</Text>
          </View>
          <Text style={[styles.cardValue, { color: colors.success }]}>
            {isConnected ? 'Available' : 'Unavailable'}
          </Text>
        </View>
      </View>

      {/* 4. CTA Button (Shown when offline or manually triggered) */}
      {!isConnected && (
        <TouchableOpacity
          style={[styles.ctaButton, { backgroundColor: colors.primary }]}
          onPress={openMobileSettings}
          activeOpacity={0.8}
        >
          <MaterialCommunityIcons name="cog-outline" size={20} color="#FFFFFF" style={{ marginRight: 8 }} />
          <Text style={styles.ctaButtonText}>Open Network Settings</Text>
        </TouchableOpacity>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'space-between',
  },
  headerRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 20,
  },
  appTitle: {
    fontSize: 26,
    fontWeight: '700',
    letterSpacing: -0.5,
  },
  appSubtitle: {
    fontSize: 14,
    fontWeight: '400',
    marginTop: 2,
  },
  activeBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#E6F9F0',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: '#A7F3D0',
  },
  badgeDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: '#00C97A',
    marginRight: 6,
  },
  badgeText: {
    color: '#00C97A',
    fontSize: 13,
    fontWeight: '600',
  },
  orbSection: {
    alignItems: 'center',
    marginVertical: 24,
  },
  orbCircle: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  connectionTitle: {
    fontSize: 28,
    fontWeight: '700',
    marginTop: 24,
    textAlign: 'center',
  },
  connectionSubtitle: {
    fontSize: 15,
    marginTop: 6,
    textAlign: 'center',
  },
  card: {
    borderRadius: 20,
    padding: 16,
    borderWidth: 1,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.05,
    shadowRadius: 10,
    elevation: 3,
    marginBottom: 16,
  },
  cardRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 8,
  },
  cardRowLeft: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  iconContainer: {
    width: 36,
    height: 36,
    borderRadius: 18,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 12,
  },
  cardLabel: {
    fontSize: 16,
    fontWeight: '600',
  },
  cardValue: {
    fontSize: 15,
    fontWeight: '700',
  },
  divider: {
    height: 1,
    marginVertical: 4,
  },
  ctaButton: {
    flexDirection: 'row',
    height: 52,
    borderRadius: 16,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 8,
  },
  ctaButtonText: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '600',
  },
});
