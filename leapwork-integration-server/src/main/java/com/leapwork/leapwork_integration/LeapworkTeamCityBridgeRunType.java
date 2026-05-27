package com.leapwork.leapwork_integration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.URI;

import org.jetbrains.annotations.NotNull;

import jetbrains.buildServer.requirements.Requirement;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.RunType;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import jetbrains.buildServer.web.ContentSecurityPolicyConfig;
import jetbrains.buildServer.web.openapi.PluginDescriptor;

public class LeapworkTeamCityBridgeRunType extends RunType {
	private final PluginDescriptor pluginDescriptor;
	private final ContentSecurityPolicyConfig cspConfiguration;
	
	public LeapworkTeamCityBridgeRunType(@NotNull final RunTypeRegistry runTypeRegistry,
			final PluginDescriptor pluginDescriptor, ContentSecurityPolicyConfig cspConfig) {
		this.pluginDescriptor = pluginDescriptor;
		this.cspConfiguration = cspConfig;
		runTypeRegistry.registerRunType(this);
	}

	@NotNull
	@Override
	public String getType() {
		return StringConstants.RunTypeName;
	}

	@NotNull
	@Override
	public String getDisplayName() {
		return "Leapwork Integration";
	}

	@NotNull
	@Override
	public String getDescription() {
		return "Integrates Leapwork codeless test automation with Teamcity. Run tests, get results, generate reports.";
	}

	@Override
	public PropertiesProcessor getRunnerPropertiesProcessor() {
		return new PropertiesProcessor() {
			public Collection<InvalidProperty> process(Map<String, String> properties) {
				ArrayList<InvalidProperty> toReturn = new ArrayList<InvalidProperty>();
				return toReturn;
			}	
		};
	}

	@Override
	public String getEditRunnerParamsJspFilePath() {
		return pluginDescriptor.getPluginResourcesPath("editRunnerParameters.jsp");
	}

	@Override
	public String getViewRunnerParamsJspFilePath() {
		return pluginDescriptor.getPluginResourcesPath("viewRunnerParameters.jsp");
	}

	@Override
	public Map<String, String> getDefaultRunnerProperties() {
		HashMap<String, String> defaults = new HashMap<String, String>();
		defaults.put(StringConstants.ParameterName_DoneStatus, "Success");
		defaults.put(StringConstants.ParameterName_TimeDelay, "3");
		defaults.put(StringConstants.ParameterName_Port, "9001");
		defaults.put(StringConstants.ParameterName_Report, "report.xml");
		return defaults;
	}

	@NotNull
	@Override
	public List<Requirement> getRunnerSpecificRequirements(@NotNull Map<String, String> runParameters) {
		List<Requirement> requirements = new ArrayList<Requirement>(super.getRunnerSpecificRequirements(runParameters));
		return requirements;

	}

	@NotNull
	@Override
	public String describeParameters(@NotNull Map<String, String> parameters) {
		StringBuilder sb = new StringBuilder();
		sb.append("\nLeapwork Controller Host: ");
		sb.append(parameters.get(StringConstants.ParameterName_Hostname));
		sb.append("\nLeapwork Controller Port: ");
		sb.append(parameters.get(StringConstants.ParameterName_Port));
		sb.append("\nAccess Key: ");
		sb.append(parameters.get(StringConstants.ParameterName_AccessKey));
		sb.append("\nReport File: ");
		sb.append(parameters.get(StringConstants.ParameterName_Report));
		sb.append("\nTime Delay: ");
		sb.append(parameters.get(StringConstants.ParameterName_TimeDelay));
		sb.append("\nDone Status As:");
		sb.append(parameters.get(StringConstants.ParameterName_DoneStatus));
		sb.append("\nWrite keyframes of passed flows: ");
		sb.append(Utils.defaultBooleanIfNull(parameters.get(StringConstants.ParameterName_PassedKeyframes), false));
		sb.append("\nSchedule Variables: ");
		sb.append(parameters.get(StringConstants.ParameterName_ScheduleVariables));
		sb.append("\nSchedule Names: ");
		sb.append(parameters.get(StringConstants.ParameterName_ScheduleNames));
		sb.append("\nSchedule Ids: ");
		sb.append(parameters.get(StringConstants.ParameterName_ScheduleIds));
		String urlValue = buildControllerApiUrl(parameters);
		cspConfiguration.addDirectiveItems("connect-src", urlValue);
		return sb.toString();
	}

	private String buildControllerApiUrl(Map<String, String> parameters) {
		boolean enableHttps = Utils.defaultBooleanIfNull(parameters.get(StringConstants.ParameterName_HTTPS), false);
		String scheme = enableHttps ? "https" : "http";
		int port = enableHttps ? 9002 : 9001;
		try {
			String rawPort = parameters.get(StringConstants.ParameterName_Port);
			if (rawPort != null && rawPort.isEmpty() == false)
				port = Integer.parseInt(rawPort);
		} catch (Exception ignored) {
		}

		try {
			String hostname = parameters.get(StringConstants.ParameterName_Hostname);
			String normalizedHostname = normalizeUrlQuerySeparators(hostname == null ? "" : hostname.trim());
			String candidate = normalizedHostname.startsWith("http://") || normalizedHostname.startsWith("https://")
					? normalizedHostname
					: (normalizedHostname.contains("/") || normalizedHostname.contains("?")
							? scheme + "://" + normalizedHostname
							: scheme + "://" + normalizedHostname + ":" + port);
			URI parsedUri = new URI(candidate);
			String resolvedPath = parsedUri.getPath() == null || parsedUri.getPath().trim().isEmpty() ? "/" : parsedUri.getPath();
			int resolvedPort = parsedUri.getPort() == -1 ? port : parsedUri.getPort();
			URI baseUri = new URI(parsedUri.getScheme(), null, parsedUri.getHost(), resolvedPort, resolvedPath,
					parsedUri.getQuery(), null);
			String normalizedBasePath = baseUri.getPath().replaceAll("/+$", "");
			return new URI(baseUri.getScheme(), null, baseUri.getHost(), baseUri.getPort(),
					normalizedBasePath + "/api/v4/schedules", baseUri.getQuery(), null).toString();
		} catch (Exception e) {
			return scheme + "://" + parameters.get(StringConstants.ParameterName_Hostname) + ":"
					+ parameters.get(StringConstants.ParameterName_Port) + "/api/v4/schedules";
		}
	}

	private String normalizeUrlQuerySeparators(String input) {
		if (input == null || input.trim().isEmpty())
			return input;

		int firstQuestionMarkIndex = input.indexOf('?');
		if (firstQuestionMarkIndex < 0)
			return input;

		String pathPart = input.substring(0, firstQuestionMarkIndex + 1);
		String queryPart = input.substring(firstQuestionMarkIndex + 1).replace("?", "&");
		return pathPart + queryPart;
	}

}
